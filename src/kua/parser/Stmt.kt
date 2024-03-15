package kua.parser

import kua.script.Memory
import kua.lexer.Location

sealed class Stmt(private val location: Location) {
    val trace get() = Trace(qualifier, location)
    
    private val qualifier get() = "${javaClass.simpleName}$disambiguation"
    
    open val disambiguation = ""
    
    abstract fun <X> accept(visitor: Visitor<X>): X
    
    data class Trace(val qualifier: String, val location: Location)
    
    object None : Stmt(Location()) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitNoneStmt(this)
    }
    
    class Block(location: Location, val stmts: List<Stmt>) : Stmt(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitBlockStmt(this)
    }
    
    class If(location: Location, val branches: List<Branch>, val elze: Stmt) : Stmt(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitIfStmt(this)
        
        data class Branch(val location: Location, val test: Expr, val body: Stmt)
    }
    
    class While(location: Location, val test: Expr, val block: Block) : Stmt(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitWhileStmt(this)
    }
    
    class Repeat(location: Location, val test: Expr, val block: Block) : Stmt(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitRepeatStmt(this)
    }
    
    class For(location: Location, val start: Expr, val end: Expr, val step: Expr, val block: Block) : Stmt(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitForStmt(this)
    }
    
    class ForIn(location: Location, val pointer: Expr.Name, val iterable: Expr, val block: Block) : Stmt(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitForInStmt(this)
    }
    
    class Break(location: Location) : Stmt(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitBreakStmt(this)
    }
    
    class Function(location: Location, val name: Expr.Name, val params: List<Expr.Name>, val block: Block) : Stmt(location) {
        lateinit var scope: Memory.Scope
        
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitFunctionStmt(this)
    }
    
    class Return(location: Location, val exprs: List<Expr>) : Stmt(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitReturnStmt(this)
    }
    
    class Assign(location: Location, val isLocal: Boolean, val names: List<Expr.Name>, val exprs: List<Expr>) : Stmt(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitAssignStmt(this)
    }
    
    class Expression(location: Location, val expr: Expr) : Stmt(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitExpressionStmt(this)
    }
    
    interface Visitor<X> {
        fun visit(stmt: Stmt) =
            stmt.accept(this)
        
        fun visitNoneStmt(stmt: None): X
        
        fun visitBlockStmt(stmt: Block): X
        
        fun visitIfStmt(stmt: If): X
        
        fun visitWhileStmt(stmt: While): X
        
        fun visitRepeatStmt(stmt: Repeat): X
        
        fun visitForStmt(stmt: For): X
        
        fun visitForInStmt(stmt: ForIn): X
        
        fun visitBreakStmt(stmt: Break): X
        
        fun visitFunctionStmt(stmt: Function): X
        
        fun visitReturnStmt(stmt: Return): X
        
        fun visitAssignStmt(stmt: Assign): X
        
        fun visitExpressionStmt(stmt: Expression): X
    }
}