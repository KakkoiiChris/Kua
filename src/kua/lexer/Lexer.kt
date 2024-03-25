package kua.lexer

import kua.KuaNumber
import kua.KuaString
import kua.Source
import kua.lexer.TokenType.*
import kua.util.illegalCharacter
import kua.util.illegalString
import kua.util.invalidEscape

class Lexer(private val source: Source) : Iterator<Token<*>> {
    companion object {
        private const val NUL = '\u0000'

        private fun isHexadecimalDigit(char: Char) =
            char.isDigit() || char in "AaBbCcDdEeFf"
    }

    private var position = 0
    private var row = 1
    private var column = 1

    override fun hasNext() = position <= source.text.length

    override fun next(): Token<*> {
        while (!atEndOfFile()) {
            if (match(Char::isWhitespace)) {
                skipWhitespace()

                continue
            }

            if (match("--[")) {
                skipBlockComment()

                continue
            }

            if (match("--")) {
                skipLineComment()

                continue
            }

            return when {
                match(Char::isJavaIdentifierStart) -> word()

                match("0x") || match("0X")         -> hexadecimalNumber(look(2))

                match(Char::isDigit)               -> number()

                match { it in "'\"" }              -> string()

                match("[[")                        -> literalString()

                else                               -> symbol()
            }
        }

        return Token(here(), EndOfFile)
    }

    private fun peek(offset: Int = 0) =
        if (position + offset in source.text.indices)
            source.text[position + offset]
        else
            NUL

    private fun look(length: Int = 1) = buildString {
        repeat(length) { i ->
            append(peek(i))
        }
    }

    private fun match(char: Char) =
        peek() == char

    private fun match(string: String) =
        look(string.length) == string

    private fun match(predicate: (Char) -> Boolean) =
        predicate(peek())

    private fun step(distance: Int = 1) = repeat(distance) {
        if (match('\n')) {
            row++

            column = 1
        }
        else column++

        position++
    }

    private fun skip(char: Char) =
        if (match(char)) {
            step()

            true
        }
        else false

    private fun skip(string: String) =
        if (match(string)) {
            step(string.length)

            true
        }
        else false

    private fun skip(predicate: Char.() -> Boolean) =
        if (match(predicate)) {
            step()

            true
        }
        else false

    private fun mustSkip(char: Char) {
        if (!skip(char)) {
            illegalCharacter(char, here())
        }
    }

    private fun mustSkip(string: String) {
        if (!skip(string)) {
            illegalString(string, here())
        }
    }

    private fun atEndOfFile() =
        match(NUL)

    private fun here() =
        Context(source.name, row, column, 1)

    private fun skipWhitespace() {
        while (skip(Char::isWhitespace)) Unit
    }

    private fun skipLineComment() {
        while (!atEndOfFile() && !skip('\n')) {
            step()
        }
    }

    private fun skipBlockComment() {
        mustSkip("--[")

        var level = 0

        while (!skip('[')) {
            if (!skip('=')) TODO()

            level++
        }

        val endBracket = "]${"=".repeat(level)}]"

        while (!atEndOfFile() && !skip(endBracket)) {
            step()
        }
    }

    private fun StringBuilder.take() {
        append(peek())

        step()
    }

    private fun word(): Token<*> {
        val start = here()

        val result = buildString {
            do {
                take()
            }
            while (!atEndOfFile() && match(Char::isJavaIdentifierPart))
        }

        val context = start..here()

        return when (result) {
            in Keyword -> Token(context, Keyword[result])

            in Literal -> Token(context, Literal[result])

            else       -> Token(context, Name(result))
        }
    }

    private fun hexadecimalNumber(begin: String): Token<Value<KuaNumber>> {
        val start = here()

        mustSkip(begin)

        if (!match(::isHexadecimalDigit)) TODO()

        val result = buildString {
            do {
                take()
            }
            while (match(::isHexadecimalDigit))
        }

        val context = start..here()

        val number = result.toDouble()

        return Token(context, Value(KuaNumber(number)))
    }

    private fun number(): Token<Value<KuaNumber>> {
        val start = here()

        val result = buildString {
            do {
                take()
            }
            while (match(Char::isDigit))

            if (match('.'))
                do {
                    take()
                }
                while (match(Char::isDigit))

            if (match { this in "Ee" }) {
                take()

                if (match { this in "+-" }) {
                    take()
                }

                do {
                    take()
                }
                while (match(Char::isDigit))
            }
        }

        val context = start..here()

        val number = result.toDouble()

        return Token(context, Value(KuaNumber(number)))
    }

    private fun string(): Token<Value<KuaString>> {
        val start = here()

        val quote = peek()

        mustSkip(quote)

        val string = buildString {
            while (!skip(quote)) {
                if (skip('\\')) {
                    append(when {
                        skip('a')            -> '\u0007'

                        skip('b')            -> '\b'

                        skip('f')            -> '\u000C'

                        skip('n')            -> '\n'

                        skip('r')            -> '\r'

                        skip('t')            -> '\t'

                        skip('v')            -> '\u000B'

                        skip('\\')           -> '\\'

                        skip(quote)          -> quote

                        skip('\n')           -> '\n'

                        skip('u')            -> unicode()

                        skip('x')            -> hexadecimal()

                        match(Char::isDigit) -> decimal()

                        skip('z')            -> {
                            while (skip(Char::isWhitespace)) Unit

                            continue
                        }

                        else                 -> invalidEscape(peek(), here())
                    })
                }
                else take()
            }
        }

        val context = start..here()

        return Token(context, Value(KuaString(string)))
    }

    private fun unicode(): Char {
        mustSkip('{')

        val code = buildString {
            do {
                if (!match(::isHexadecimalDigit)) TODO()

                take()
            }
            while (!skip('}'))
        }

        return code.toInt(16).toChar()
    }

    private fun hexadecimal(): Char {
        val code = buildString {
            repeat(2) {
                if (!match(::isHexadecimalDigit)) TODO()

                take()
            }
        }

        return code.toInt(16).toChar()
    }

    private fun decimal(): Char {
        val code = buildString {
            repeat(3) {
                if (!match(Char::isDigit)) TODO()

                take()
            }
        }

        return code.toInt(16).toChar()
    }

    private fun literalString(): Token<Value<KuaString>> {
        val start = here()

        mustSkip('[')

        var level = 0

        while (!skip('[')) {
            if (!skip('=')) TODO()

            level++
        }

        skip('\n')

        val endBracket = "]${"=".repeat(level)}]"

        val string = buildString {
            while (!skip(endBracket)) {
                take()
            }
        }

        val context = start..here()

        return Token(context, Value(KuaString(string)))
    }

    private fun symbol(): Token<Symbol> {
        val start = here()

        val symbol = Symbol.entries.firstOrNull { skip(it.rep) } ?: TODO()

        val context = start..here()

        return Token(context, symbol)
    }
}