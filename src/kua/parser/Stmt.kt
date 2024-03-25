package kua.parser

import kua.runtime.Memory
import kua.lexer.Context

sealed class Stmt(private val context: Context) {
    val trace get() = Trace(qualifier, context)
    
    private val qualifier get() = "${javaClass.simpleName}$disambiguation"
    
    open val disambiguation = ""
    
    abstract fun <X> accept(visitor: Visitor<X>): X
    
    data class Trace(val qualifier: String, val context: Context)
    
    data object None : Stmt(Context.none) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitNoneStmt(this)
    }
    
    class Block(context: Context, val stmts: List<Stmt>) : Stmt(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitBlockStmt(this)
    }
    
    class If(context: Context, val branches: List<Branch>, val elze: Stmt) : Stmt(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitIfStmt(this)
        
        data class Branch(val context: Context, val test: Expr, val body: Stmt)
    }
    
    class While(context: Context, val test: Expr, val block: Block) : Stmt(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitWhileStmt(this)
    }
    
    class Repeat(context: Context, val test: Expr, val block: Block) : Stmt(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitRepeatStmt(this)
    }
    
    class For(context: Context, val start: Expr, val end: Expr, val step: Expr, val block: Block) : Stmt(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitForStmt(this)
    }
    
    class ForIn(context: Context, val pointer: Expr.Name, val iterable: Expr, val block: Block) : Stmt(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitForInStmt(this)
    }
    
    class Break(context: Context) : Stmt(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitBreakStmt(this)
    }
    
    class Function(context: Context, val name: Expr.Name, val params: List<Expr.Name>, val block: Block) : Stmt(context) {
        lateinit var scope: Memory.Scope
        
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitFunctionStmt(this)
    }
    
    class Return(context: Context, val exprs: List<Expr>) : Stmt(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitReturnStmt(this)
    }
    
    class Assign(context: Context, val isLocal: Boolean, val names: List<Expr.Name>, val exprs: List<Expr>) : Stmt(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitAssignStmt(this)
    }
    
    class Expression(context: Context, val expr: Expr) : Stmt(context) {
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