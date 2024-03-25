package kua.lexer

data class Context(val name: String, val row: Int, val column: Int, val length: Int) {
    companion object {
        val none = Context("", 0, 0, 0)
    }

    operator fun rangeTo(other: Context) =
        Context(name, row, column, (other.column - column) + other.length - 1)
}