package kua.lexer

import kua.lexer.Token.Type.*
import kua.script.Nil
import kua.util.*

class Lexer(private val source: String) : Iterator<Token> {
    companion object {
        private const val NUL = '\u0000'
    }
    
    private val keywords = Token.Type.keywords
    
    private val literals = listOf(true, false, Nil)
        .associateBy(Any::toString)
    
    private var pos = 0
    private var row = 1
    private var col = 1
    
    override fun hasNext() = pos <= source.length
    
    override fun next(): Token {
        while (!atEndOfFile()) {
            if (match(Char::isWhitespace)) {
                skipWhitespace()
                
                continue
            }
            
            if (match("--[[")) {
                skipBlockComment()
                
                continue
            }
            
            if (match("--")) {
                skipLineComment()
                
                continue
            }
            
            return when {
                match(Char::isJavaIdentifierStart) -> word()
                
                match { isDigit() }                -> number()
                
                match { this in "'\"" }            -> string()
                
                match("[[")                        -> literalString()
                
                else                               -> symbol()
            }
        }
        
        return Token(here(), EndOfFile)
    }
    
    private fun peek(offset: Int = 0) =
        if (pos + offset in source.indices)
            source[pos + offset]
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
    
    private fun match(predicate: Char.() -> Boolean) =
        predicate(peek())
    
    private fun step(distance: Int = 1) = repeat(distance) {
        if (match('\n')) {
            row++
            
            col = 1
        }
        else col++
        
        pos++
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
        Location(row, col)
    
    private fun skipWhitespace() {
        while (skip(Char::isWhitespace))
            Unit
    }
    
    private fun skipLineComment() {
        while (!atEndOfFile() && !skip('\n'))
            step()
    }
    
    private fun skipBlockComment() {
        mustSkip("--[[")
        
        while (!atEndOfFile() && !skip("]]"))
            step()
    }
    
    private fun StringBuilder.take() {
        append(peek())
        
        step()
    }
    
    private fun word(): Token {
        val loc = here()
        
        val result = buildString {
            do
                take()
            while (!atEndOfFile() && match(Char::isJavaIdentifierPart))
        }
        
        return when (result) {
            in keywords.keys -> Token(loc, keywords[result]!!)
            
            in literals.keys -> Token(loc, Value, literals[result]!!)
            
            else             -> Token(loc, Name, result)
        }
    }
    
    private fun number(): Token {
        val loc = here()
        
        val result = buildString {
            do
                take()
            while (match { isDigit() })
            
            if (match('.'))
                do
                    take()
                while (match { isDigit() })
            
            if (match { this in "Ee" }) {
                take()
                
                if (match { this in "+-" })
                    take()
                
                do
                    take()
                while (match { isDigit() })
            }
        }
        
        val number = result.toDoubleOrNull() ?: invalidNumber(result, loc)
        
        return Token(loc, Value, number)
    }
    
    private fun string(): Token {
        val loc = here()
        
        val quote = peek()
        
        mustSkip(quote)
        
        val string = buildString {
            while (!skip(quote)) {
                if (skip('\\')) {
                    append(when {
                        skip('a')   -> '\u0007'
                        
                        skip('b')   -> '\b'
                        
                        skip('f')   -> '\u000C'
                        
                        skip('n')   -> '\n'
                        
                        skip('r')   -> '\r'
                        
                        skip('t')   -> '\t'
                        
                        skip('v')   -> '\u000B'
                        
                        skip('\\')  -> '\\'
                        
                        skip(quote) -> quote
                        
                        else        -> invalidEscape(peek(), loc)
                    })
                }
                else take()
            }
        }
        
        return Token(loc, Value, string)
    }
    
    private fun literalString(): Token {
        val loc = here()
        
        mustSkip("[[")
        
        skip('\n')
        
        var level = 1
        
        val string = buildString {
            while (!(look(2) == "]]" && level == 0)) {
                take()
                
                when (look(2)) {
                    "[[" -> level++
                    
                    "]]" -> level--
                }
            }
        }
        
        mustSkip("]]")
        
        return Token(loc, Value, string)
    }
    
    private fun symbol(): Token {
        val loc = here()
        
        val symbol = when {
            skip('+') -> Plus
            
            skip('-') -> Dash
            
            skip('*') -> Star
            
            skip('/') -> Slash
            
            skip('%') -> Percent
            
            skip('^') -> Caret
            
            skip('=') -> when {
                skip('=') -> DoubleEqual
                
                else      -> Equal
            }
            
            skip('~') -> when {
                skip('=') -> TildeEqual
                
                else      -> illegalCharacter('~', loc)
            }
            
            skip('<') -> when {
                skip('=') -> LessEqual
                
                else      -> Less
            }
            
            skip('>') -> when {
                skip('=') -> GreaterEqual
                
                else      -> Greater
            }
            
            skip('.') -> when {
                skip('.') -> DoubleDot
                
                else      -> Dot
            }
            
            skip('#') -> Pound
            
            skip('(') -> LeftParen
            
            skip(')') -> RightParen
            
            skip('[') -> LeftSquare
            
            skip(']') -> RightSquare
            
            skip('{') -> LeftBrace
            
            skip('}') -> RightBrace
            
            skip(',') -> Comma
            
            skip(';') -> Semicolon
            
            else      -> illegalCharacter(peek(), loc)
        }
        
        return Token(loc, symbol)
    }
}