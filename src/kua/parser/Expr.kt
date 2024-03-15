package kua.parser

import kua.lexer.Token
import kua.lexer.Location

sealed class Expr(val location: Location) {
    abstract fun <X> accept(visitor: Visitor<X>): X
    
    object None : Expr(Location()) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitNoneExpr(this)
    }
    
    class Value(location: Location, val value: Any) : Expr(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitValueExpr(this)
    }
    
    class Name(location: Location, val name: String) : Expr(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitNameExpr(this)
        
        override fun toString() =
            name
    }
    
    class Table(location: Location, val listInit: List<Expr>, val mapInit: Map<String, Expr>) : Expr(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitTableExpr(this)
    }
    
    class Unary(location: Location, val operator: Operator, val expr: Expr) : Expr(location) {
        enum class Operator(val symbol: Token.Type) {
            Negate(Token.Type.Dash),
            Not(Token.Type.Not),
            Size(Token.Type.Pound);
            
            companion object {
                operator fun get(symbol: Token.Type) =
                    values()
                        .first { it.symbol == symbol }
            }
        }
        
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitUnaryExpr(this)
    }
    
    class Binary(location: Location, val operator: Operator, val left: Expr, val right: Expr) : Expr(location) {
        enum class Operator(val symbol: Token.Type) {
            Or(Token.Type.Or),
            And(Token.Type.And),
            Equal(Token.Type.DoubleEqual),
            NotEqual(Token.Type.TildeEqual),
            Less(Token.Type.Less),
            LessEqual(Token.Type.LessEqual),
            Greater(Token.Type.Greater),
            GreaterEqual(Token.Type.GreaterEqual),
            Concat(Token.Type.DoubleDot),
            Add(Token.Type.Plus),
            Subtract(Token.Type.Dash),
            Multiply(Token.Type.Star),
            Divide(Token.Type.Slash),
            Modulo(Token.Type.Percent),
            Exponent(Token.Type.Caret);
            
            companion object {
                operator fun get(symbol: Token.Type) =
                    values()
                        .first { it.symbol == symbol }
            }
        }
        
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitBinaryExpr(this)
    }
    
    class GetIndex(location: Location, val target: Expr, val index: Expr) : Expr(location) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitGetIndexExpr(this)
    }
    
    class SetIndex(location: Location, val target: Expr, val index: Expr, val value: Expr) : Expr(location) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSetIndexExpr(this)
    }
    
    class GetMember(location: Location, val target: Expr, val member: Name) : Expr(location) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitGetMemberExpr(this)
    }
    
    class SetMember(location: Location, val target: Expr, val member: Name, val value: Expr) : Expr(location) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSetMemberExpr(this)
    }
    
    class Invoke(location: Location, val target: Expr, val args: List<Expr>) : Expr(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitInvokeExpr(this)
    }
    
    class Lambda(location: Location, val params: List<Name>, val block: Stmt.Block) : Expr(location) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitLambdaExpr(this)
    }
    
    interface Visitor<X> {
        fun visit(expr: Expr) =
            expr.accept(this)
        
        fun visitNoneExpr(expr: None): X
        
        fun visitValueExpr(expr: Value): X
        
        fun visitNameExpr(expr: Name): X
        
        fun visitTableExpr(expr: Table): X
        
        fun visitUnaryExpr(expr: Unary): X
        
        fun visitBinaryExpr(expr: Binary): X
        
        fun visitGetIndexExpr(expr: GetIndex): X
        
        fun visitSetIndexExpr(expr: SetIndex): X
        
        fun visitGetMemberExpr(expr: GetMember): X
        
        fun visitSetMemberExpr(expr: SetMember): X
        
        fun visitInvokeExpr(expr: Invoke): X
        
        fun visitLambdaExpr(expr: Lambda): X
    }
}