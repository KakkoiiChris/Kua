package kua.lexer

import kua.KuaBoolean
import kua.KuaValue
import kua.Nil

sealed interface TokenType {
    data class Value<X : KuaValue<*>>(val value: X) : TokenType

    data class Name(val value: String) : TokenType

    enum class Keyword : TokenType {
        AND,
        BREAK,
        DO,
        ELSEIF,
        ELSE,
        END,
        FOR,
        FUNCTION,
        GOTO,
        IF,
        IN,
        LOCAL,
        NOT,
        OR,
        REPEAT,
        RETURN,
        THEN,
        UNTIL,
        WHILE;

        companion object {
            operator fun contains(string: String) =
                entries.any { it.name.lowercase() == string }

            operator fun get(string: String) =
                entries.first { it.name.lowercase() == string }
        }

        override fun toString() = name.lowercase()
    }

    enum class Literal(value: KuaValue<*>) : TokenType {
        TRUE(KuaBoolean(true)),
        FALSE(KuaBoolean(false)),
        NIL(Nil);

        companion object {
            operator fun contains(string: String) =
                Keyword.entries.any { it.name.lowercase() == string }

            operator fun get(string: String) =
                Keyword.entries.first { it.name.lowercase() == string }
        }

        val value = Value(value)

        override fun toString() = value.toString()
    }

    enum class Symbol(val rep: String) : TokenType {
        PLUS("+"),
        DASH("-"),
        STAR("*"),
        DOUBLE_SLASH("//"),
        SLASH("/"),
        PERCENT("%"),
        CARET("^"),
        POUND("#"),
        AMPERSAND("&"),
        TILDE_EQUAL("~="),
        TILDE("~"),
        PIPE("|"),
        DOUBLE_LESS("<<"),
        DOUBLE_GREATER(">>"),
        DOUBLE_EQUAL("=="),
        LESS_EQUAL("<="),
        GREATER_EQUAL(">="),
        LESS("<"),
        GREATER(">"),
        EQUAL("="),
        LEFT_PAREN("("),
        RIGHT_PAREN(")"),
        LEFT_BRACE("{"),
        RIGHT_BRACE("}"),
        LEFT_SQUARE("["),
        RIGHT_SQUARE("]"),
        DOUBLE_COLON("::"),
        SEMICOLON(";"),
        COLON(":"),
        COMMA(","),
        TRIPLE_DOT("..."),
        DOUBLE_DOT(".."),
        DOT(".");

        override fun toString() = rep
    }

    data object EndOfFile : TokenType
}