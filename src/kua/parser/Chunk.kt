package kua.parser

data class Chunk(val stmts: List<Stmt>) : Iterator<Stmt> by stmts.iterator()