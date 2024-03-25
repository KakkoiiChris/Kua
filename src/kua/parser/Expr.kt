package kua.parser

import kua.KuaString
import kua.KuaValue
import kua.lexer.Context
import kua.lexer.TokenType
import kua.lexer.TokenType.*

sealed class Expr(val context: Context) {
    abstract fun <X> accept(visitor: Visitor<X>): X

    class Value(context: Context, val value: KuaValue<*>) : Expr(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitValueExpr(this)
    }

    class Name(context: Context, val name: String) : Expr(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitNameExpr(this)

        fun toValue() =
            Value(context, KuaString(name))

        override fun toString() =
            name
    }

    class Table(context: Context, val listInit: List<Expr>, val mapInit: Map<String, Expr>) : Expr(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitTableExpr(this)
    }

    class Unary(context: Context, val operator: Operator, val expr: Expr) : Expr(context) {
        enum class Operator(val symbol: TokenType) {
            Negate(Symbol.DASH),
            Not(Keyword.NOT),
            Size(Symbol.POUND);

            companion object {
                operator fun get(symbol: TokenType) =
                    entries.first { it.symbol == symbol }
            }
        }

        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitUnaryExpr(this)
    }

    class Binary(context: Context, val operator: Operator, val left: Expr, val right: Expr) : Expr(context) {
        enum class Operator(val symbol: TokenType) {
            Or(Keyword.OR),
            And(Keyword.AND),
            Equal(Symbol.DOUBLE_EQUAL),
            NotEqual(Symbol.TILDE_EQUAL),
            Less(Symbol.LESS),
            LessEqual(Symbol.LESS_EQUAL),
            Greater(Symbol.GREATER),
            GreaterEqual(Symbol.GREATER_EQUAL),
            Concat(Symbol.DOUBLE_DOT),
            Add(Symbol.PLUS),
            Subtract(Symbol.DASH),
            Multiply(Symbol.STAR),
            Divide(Symbol.SLASH),
            IntDivide(Symbol.DOUBLE_SLASH),
            Modulo(Symbol.PERCENT),
            Exponent(Symbol.CARET);

            companion object {
                operator fun get(symbol: TokenType) =
                    entries.first { it.symbol == symbol }
            }
        }

        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitBinaryExpr(this)
    }

    class GetIndex(context: Context, val target: Expr, val index: Expr) : Expr(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitGetIndexExpr(this)
    }

    class SetIndex(context: Context, val target: Expr, val index: Expr, val value: Expr) : Expr(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSetIndexExpr(this)
    }

    class Invoke(context: Context, val target: Expr, val args: List<Expr>) : Expr(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitInvokeExpr(this)
    }

    class Lambda(context: Context, val params: List<Name>, val block: Stmt.Block) : Expr(context) {
        override fun <X> accept(visitor: Visitor<X>) =
            visitor.visitLambdaExpr(this)
    }

    interface Visitor<X> {
        fun visit(expr: Expr) =
            expr.accept(this)

        fun visitValueExpr(expr: Value): X

        fun visitNameExpr(expr: Name): X

        fun visitTableExpr(expr: Table): X

        fun visitUnaryExpr(expr: Unary): X

        fun visitBinaryExpr(expr: Binary): X

        fun visitGetIndexExpr(expr: GetIndex): X

        fun visitSetIndexExpr(expr: SetIndex): X

        fun visitInvokeExpr(expr: Invoke): X

        fun visitLambdaExpr(expr: Lambda): X
    }
}