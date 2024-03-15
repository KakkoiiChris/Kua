package kua.lexer

data class Location(val row: Int = 0, val col: Int = 0) {
    override fun toString() =
        if (row < 1)
            ""
        else
            " (Row $row, Col $col)"
}