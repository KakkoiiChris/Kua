package kua.runtime

import kua.*
import kua.lexer.Context
import kua.parser.Chunk
import kua.parser.Expr
import kua.parser.Stmt
import kua.util.*
import kotlin.math.min
import kotlin.math.pow

class Runtime(private val chunk: Chunk) : Expr.Visitor<KuaValue<*>>, Stmt.Visitor<Unit> {
    private val memory = Memory()

    fun run(): List<KuaValue<*>> {
        try {
            for (stmt in chunk) {
                visit(stmt)
            }
        }
        catch (r: Return) {
            r.value
        }

        return emptyList()
    }

    private fun Any.toBoolean() =
        this != false && this != Nil

    private fun Any.toNumberOrNull() =
        when (this) {
            is Double -> this

            is String -> toDoubleOrNull()

            else      -> null
        }

    override fun visitValueExpr(expr: Expr.Value) =
        expr.value

    override fun visitNameExpr(expr: Expr.Name) =
        memory[expr]

    override fun visitTableExpr(expr: Expr.Table): KuaTable {
        val table = Table()

        for (node in expr.listInit) {
            table += visit(node)
        }

        for ((name, subExpr) in expr.mapInit) {
            table[name] = visit(subExpr)
        }

        return KuaTable(table)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): KuaValue<*> {
        return when (expr.operator) {
            Expr.Unary.Operator.Negate -> when (val e = visit(expr.expr)) {
                is KuaNumber -> KuaNumber(-e.value)

                else         -> invalidUnaryOperand(e, expr.operator, expr.expr.context)
            }

            Expr.Unary.Operator.Not    -> KuaBoolean(!visit(expr.expr).toBoolean())

            Expr.Unary.Operator.Size   -> KuaNumber(when (val e = visit(expr.expr)) {
                is KuaString -> e.value.length.toDouble()

                is KuaTable  -> e.value.length.toDouble()

                else         -> invalidUnaryOperand(e, expr.operator, expr.expr.context)
            })
        }
    }

    override fun visitBinaryExpr(expr: Expr.Binary): KuaValue<*> {
        return when (expr.operator) {
            Expr.Binary.Operator.Or           -> {
                val l = visit(expr.left)

                if (l.toBoolean()) {
                    l
                }
                else {
                    visit(expr.right)
                }
            }

            Expr.Binary.Operator.And          -> {
                val l = visit(expr.left)

                if (!l.toBoolean()) {
                    l
                }
                else {
                    visit(expr.right)
                }
            }

            Expr.Binary.Operator.Equal        -> when (val l = visit(expr.left)) {
                is Boolean -> when (val r = visit(expr.right)) {
                    is Boolean -> l == r

                    else       -> false
                }

                is Double  -> when (val r = visit(expr.right)) {
                    is Double -> l == r

                    else      -> false
                }

                is String  -> when (val r = visit(expr.right)) {
                    is String -> l == r

                    else      -> false
                }

                Nil        -> visit(expr.right) is Nil

                else       -> false
            }

            Expr.Binary.Operator.NotEqual     -> when (val l = visit(expr.left)) {
                is Boolean -> when (val r = visit(expr.right)) {
                    is Boolean -> l != r

                    else       -> true
                }

                is Double  -> when (val r = visit(expr.right)) {
                    is Double -> l != r

                    else      -> true
                }

                is String  -> when (val r = visit(expr.right)) {
                    is String -> l != r

                    else      -> true
                }

                Nil        -> visit(expr.right) !is Nil

                else       -> true
            }

            Expr.Binary.Operator.Less         -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l < r

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                is String -> when (val r = visit(expr.right)) {
                    is String -> l < r

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                else      -> invalidLeftOperand(l, expr.operator, expr.left.context)
            }

            Expr.Binary.Operator.LessEqual    -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l <= r

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                is String -> when (val r = visit(expr.right)) {
                    is String -> l <= r

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                else      -> invalidLeftOperand(l, expr.operator, expr.left.context)
            }

            Expr.Binary.Operator.Greater      -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l > r

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                is String -> when (val r = visit(expr.right)) {
                    is String -> l > r

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                else      -> invalidLeftOperand(l, expr.operator, expr.left.context)
            }

            Expr.Binary.Operator.GreaterEqual -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l >= r
                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                is String -> when (val r = visit(expr.right)) {
                    is String -> l >= r
                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                else      -> invalidLeftOperand(l, expr.operator, expr.left.context)
            }

            Expr.Binary.Operator.Concat       -> {
                val l = visit(expr.left).truncate()
                val r = visit(expr.right).truncate()

                "$l$r"
            }

            Expr.Binary.Operator.Add          -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l + r

                    is String -> l + (r.toNumberOrNull() ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)) + r

                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)) + (r.toNumberOrNull()
                        ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                else      -> invalidLeftOperand(l, expr.operator, expr.left.context)
            }

            Expr.Binary.Operator.Subtract     -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l - r

                    is String -> l - (r.toNumberOrNull() ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)) - r

                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)) - (r.toNumberOrNull()
                        ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                else      -> invalidLeftOperand(l, expr.operator, expr.left.context)
            }

            Expr.Binary.Operator.Multiply     -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l * r

                    is String -> l * (r.toNumberOrNull() ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)) * r

                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)) * (r.toNumberOrNull()
                        ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                else      -> invalidLeftOperand(l, expr.operator, expr.left.context)
            }

            Expr.Binary.Operator.Divide       -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l / r

                    is String -> l / (r.toNumberOrNull() ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)) / r

                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)) / (r.toNumberOrNull()
                        ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                else      -> invalidLeftOperand(l, expr.operator, expr.left.context)
            }

            Expr.Binary.Operator.Modulo       -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l % r

                    is String -> l % (r.toNumberOrNull() ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)) % r

                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)) % (r.toNumberOrNull()
                        ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                else      -> invalidLeftOperand(l, expr.operator, expr.left.context)
            }

            Expr.Binary.Operator.Exponent     -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l.pow(r)

                    is String -> l.pow(r.toNumberOrNull() ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)).pow(r)

                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.context)).pow(r.toNumberOrNull()
                        ?: numberCoercion(r, expr.right.context))

                    else      -> invalidRightOperand(r, expr.operator, expr.right.context)
                }

                else      -> invalidLeftOperand(l, expr.operator, expr.left.context)
            }
        }
    }

    override fun visitGetIndexExpr(expr: Expr.GetIndex) =
        when (val target = visit(expr.target)) {
            Nil          -> Nil

            is KuaString -> when (val index = visit(expr.index)) {
                is KuaNumber -> KuaString(target.value[index.value.toInt() - 1].toString())

                else         -> invalidStringIndex(index, expr.index.context)
            }

            is KuaTable  -> when (val index = visit(expr.index)) {
                Nil          -> Nil

                is KuaNumber -> target.value[index.value]

                is KuaString -> target.value[index.value]

                else         -> invalidTableIndex(index, expr.index.context)
            }

            else         -> nonIndexedValue(target, expr.target.context)
        }

    override fun visitSetIndexExpr(expr: Expr.SetIndex): Any {
        TODO("Not yet implemented")
    }

    override fun visitInvokeExpr(expr: Expr.Invoke): KuaValue<*> {
        val target = visit(expr.target) as? Stmt.Function ?: TODO()

        val min = min(target.params.size, expr.args.size)

        try {
            memory.push(Memory.Scope(target.scope))

            for ((i, param) in target.params.withIndex()) {
                memory[param, true] = if (i in 0 until min) {
                    visit(expr.args[i])
                }
                else {
                    Nil
                }
            }

            visit(target.block)
        }
        catch (x: Return) {
            return x.value
        }
        finally {
            memory.pop()
        }

        return Unit
    }

    override fun visitLambdaExpr(expr: Expr.Lambda): KuaFunction {
        val function = Stmt.Function(expr.context, Expr.Name.none, expr.params, expr.block)

        function.scope = memory.peek()

        return function
    }

    override fun visitNoneStmt(stmt: Stmt.None) = Unit

    override fun visitBlockStmt(stmt: Stmt.Block) {
        try {
            memory.push()

            for (subStmt in stmt.stmts) {
                visit(subStmt)
            }
        }
        finally {
            memory.pop()
        }
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        for (branch in stmt.branches) {
            if (visit(branch.test).toBoolean()) {
                visit(branch.body)

                return
            }
        }

        visit(stmt.elze)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (visit(stmt.test).toBoolean()) {
            visit(stmt.block)
        }
    }

    override fun visitRepeatStmt(stmt: Stmt.Repeat) {
        do {
            visit(stmt.block)
        }
        while (visit(stmt.test).toBoolean())
    }

    override fun visitForStmt(stmt: Stmt.For) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitForInStmt(stmt: Stmt.ForIn) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        throw Break
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        stmt.scope = memory.peek()

        memory[stmt.name, false] = stmt
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val values = mutableListOf<KuaValue<*>>()

        for (expr in stmt.exprs) {
            values += visit(expr)
        }

        val value = if (values.size == 1) {
            values[0]
        }
        else {
            KuaTuple(values)
        }

        throw Return(value)
    }

    override fun visitAssignStmt(stmt: Stmt.Assign) {
        val values = mutableListOf<Any>()

        for (expr in stmt.exprs) {
            val result = visit(expr)

            if (result is Return.Tuple)
                values.addAll(result.values)
            else
                values += result
        }

        while (values.size < stmt.names.size) {
            values += Nil
        }

        while (values.size > stmt.names.size) {
            values.removeLast()
        }

        for (i in stmt.names.indices) {
            val name = stmt.names[i]
            val value = values[i]

            memory[name, stmt.isLocal] = value
        }
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        visit(stmt.expr)
    }
}