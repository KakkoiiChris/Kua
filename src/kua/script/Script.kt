package kua.script

import kua.lexer.Location
import kua.parser.Chunk
import kua.parser.Expr
import kua.parser.Stmt
import kua.util.*
import kotlin.math.min
import kotlin.math.pow

class Script(private val chunk: Chunk) : Expr.Visitor<Any>, Stmt.Visitor<Unit> {
    private val memory = Memory()
    
    fun run(): List<Any> {
        try {
            for (stmt in chunk) {
                visit(stmt)
            }
        }
        catch (r: Return) {
            return when (val value = r.value) {
                is Return.Tuple -> value.values
                
                else            -> listOf(value)
            }
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
    
    override fun visitNoneExpr(expr: Expr.None) =
        Unit
    
    override fun visitValueExpr(expr: Expr.Value) =
        expr.value
    
    override fun visitNameExpr(expr: Expr.Name) =
        memory[expr]
    
    override fun visitTableExpr(expr: Expr.Table): Table {
        val table = Table()
        
        for (node in expr.listInit) {
            table += visit(node)
        }
        
        for ((name, subExpr) in expr.mapInit) {
            table[name] = visit(subExpr)
        }
        
        return table
    }
    
    override fun visitUnaryExpr(expr: Expr.Unary): Any {
        return when (expr.operator) {
            Expr.Unary.Operator.Negate -> when (val e = visit(expr.expr)) {
                is Double -> -e
                
                else      -> invalidUnaryOperand(e, expr.operator, expr.expr.location)
            }
            
            Expr.Unary.Operator.Not    -> !visit(expr.expr).toBoolean()
            
            Expr.Unary.Operator.Size   -> when (val e = visit(expr.expr)) {
                is String -> e.length.toDouble()
                
                is Table  -> e.length.toDouble()
                
                else      -> invalidUnaryOperand(e, expr.operator, expr.expr.location)
            }
        }
    }
    
    override fun visitBinaryExpr(expr: Expr.Binary): Any {
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
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                is String -> when (val r = visit(expr.right)) {
                    is String -> l < r
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                else      -> invalidLeftOperand(l, expr.operator, expr.left.location)
            }
            
            Expr.Binary.Operator.LessEqual    -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l <= r
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                is String -> when (val r = visit(expr.right)) {
                    is String -> l <= r
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                else      -> invalidLeftOperand(l, expr.operator, expr.left.location)
            }
            
            Expr.Binary.Operator.Greater      -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l > r
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                is String -> when (val r = visit(expr.right)) {
                    is String -> l > r
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                else      -> invalidLeftOperand(l, expr.operator, expr.left.location)
            }
            
            Expr.Binary.Operator.GreaterEqual -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l >= r
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                is String -> when (val r = visit(expr.right)) {
                    is String -> l >= r
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                else      -> invalidLeftOperand(l, expr.operator, expr.left.location)
            }
            
            Expr.Binary.Operator.Concat       -> {
                val l = visit(expr.left).truncate()
                val r = visit(expr.right).truncate()
                
                "$l$r"
            }
            
            Expr.Binary.Operator.Add          -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l + r
                    
                    is String -> l + (r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)) + r
                    
                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)) + (r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                else      -> invalidLeftOperand(l, expr.operator, expr.left.location)
            }
            
            Expr.Binary.Operator.Subtract     -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l - r
                    
                    is String -> l - (r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)) - r
                    
                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)) - (r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                else      -> invalidLeftOperand(l, expr.operator, expr.left.location)
            }
            
            Expr.Binary.Operator.Multiply     -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l * r
                    
                    is String -> l * (r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)) * r
                    
                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)) * (r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                else      -> invalidLeftOperand(l, expr.operator, expr.left.location)
            }
            
            Expr.Binary.Operator.Divide       -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l / r
                    
                    is String -> l / (r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)) / r
                    
                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)) / (r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                else      -> invalidLeftOperand(l, expr.operator, expr.left.location)
            }
            
            Expr.Binary.Operator.Modulo       -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l % r
                    
                    is String -> l % (r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)) % r
                    
                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)) % (r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                else      -> invalidLeftOperand(l, expr.operator, expr.left.location)
            }
            
            Expr.Binary.Operator.Exponent     -> when (val l = visit(expr.left)) {
                is Double -> when (val r = visit(expr.right)) {
                    is Double -> l.pow(r)
                    
                    is String -> l.pow(r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                is String -> when (val r = visit(expr.right)) {
                    is Double -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)).pow(r)
                    
                    is String -> (l.toNumberOrNull() ?: numberCoercion(l, expr.left.location)).pow(r.toNumberOrNull() ?: numberCoercion(r, expr.right.location))
                    
                    else      -> invalidRightOperand(r, expr.operator, expr.right.location)
                }
                
                else      -> invalidLeftOperand(l, expr.operator, expr.left.location)
            }
        }
    }
    
    override fun visitGetIndexExpr(expr: Expr.GetIndex) =
        when (val target = visit(expr.target)) {
            Nil       -> Nil
            
            is String -> when (val index = visit(expr.index)) {
                is Double -> target[index.toInt() - 1].toString()
                
                else      -> invalidStringIndex(index, expr.index.location)
            }
            
            is Table  -> when (val index = visit(expr.index)) {
                Nil       -> Nil
                
                is Double -> target[index]
                
                is String -> target[index]
                
                else      -> invalidTableIndex(index, expr.index.location)
            }
            
            else      -> nonIndexedValue(target, expr.target.location)
        }
    
    override fun visitSetIndexExpr(expr: Expr.SetIndex): Any {
        TODO("Not yet implemented")
    }
    
    override fun visitGetMemberExpr(expr: Expr.GetMember) =
        when (val target = visit(expr.target)) {
            Nil      -> Nil
            
            is Table -> target[expr.member]
            
            else     -> nonAccessedValue(target, expr.target.location)
        }
    
    override fun visitSetMemberExpr(expr: Expr.SetMember): Any {
        TODO("Not yet implemented")
    }
    
    override fun visitInvokeExpr(expr: Expr.Invoke): Any {
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
    
    override fun visitLambdaExpr(expr: Expr.Lambda): Any {
        val function = Stmt.Function(expr.location, Expr.Name(Location(), ""), expr.params, expr.block)
        
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
        val values = mutableListOf<Any>()
        
        for (expr in stmt.exprs) {
            values += visit(expr)
        }
        
        val value = if (values.size == 1) {
            values[0]
        }
        else {
            Return.Tuple(values)
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