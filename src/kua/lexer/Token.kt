package kua.lexer

data class Token(val location: Location, val type: Type, val value: Any = Unit) {
    enum class Type(private val symbol: String) {
        Value("<V>"),
        Name("<N>"),
        Plus("+"),
        Dash("-"),
        Star("*"),
        Slash("/"),
        Percent("%"),
        Caret("^"),
        Equal("="),
        DoubleEqual("=="),
        TildeEqual("~="),
        Less("<"),
        LessEqual("<="),
        Greater(">"),
        GreaterEqual(">="),
        And("and"),
        Or("or"),
        Not("not"),
        DoubleDot(".."),
        Pound("#"),
        LeftParen("("),
        RightParen(")"),
        LeftSquare("["),
        RightSquare("]"),
        LeftBrace("{"),
        RightBrace("}"),
        Comma(","),
        Dot("."),
        Semicolon(";"),
        Function("function"),
        Local("local"),
        If("if"),
        Then("then"),
        ElseIf("elseif"),
        Else("else"),
        While("while"),
        Repeat("repeat"),
        Until("until"),
        For("for"),
        In("in"),
        Do("do"),
        Break("break"),
        Return("return"),
        End("end"),
        EndOfFile("<0>");
        
        companion object {
            val keywords
                get() = values()
                    .filter { it.symbol.all(Char::isLetter) }
                    .associateBy { it.symbol }
        }
        
        override fun toString() = symbol
    }
}