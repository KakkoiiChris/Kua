package kua.lexer

data class Token<T : TokenType>(val context: Context, val type: T)