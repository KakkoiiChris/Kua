package kua.parser

import kua.lexer.Lexer
import kua.lexer.Token
import kua.lexer.TokenType
import kua.lexer.TokenType.*
import kua.util.*

class Parser(private val lexer: Lexer) {
    private var currentToken = lexer.next()

    fun parse(): Chunk {
        val stmts = mutableListOf<Stmt>()

        while (!skip(EndOfFile)) {
            stmts += stmt()
        }

        return Chunk(stmts)
    }

    private fun peek() = currentToken

    private fun step() {
        if (lexer.hasNext()) {
            currentToken = lexer.next()
        }
    }

    private fun match(type: TokenType) =
        peek().type == type

    private inline fun <reified X : TokenType> match() =
        X::class.isInstance(peek().type)

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified X : TokenType> get(): Token<X> =
        peek() as Token<X>

    private fun matchAny(vararg types: TokenType) =
        types.any { match(it) }

    private fun skip(type: TokenType) =
        if (match(type)) {
            step()

            true
        }
        else false

    private fun mustSkip(type: TokenType) {
        if (!skip(type)) {
            invalidType(type, peek().type, here())
        }
    }

    private fun skipSemicolons() {
        while (skip(Symbol.SEMICOLON)) Unit
    }

    private fun here() =
        peek().context

    private fun stmt(): Stmt {
        val stmt = when {
            match(Keyword.DO)       -> doStmt()

            match(Keyword.IF)       -> ifStmt()

            match(Keyword.WHILE)    -> whileStmt()

            match(Keyword.REPEAT)   -> repeatStmt()

            match(Keyword.FOR)      -> forStmt()

            match(Keyword.BREAK)    -> breakStmt()

            match(Keyword.FUNCTION) -> functionStmt()

            match(Keyword.RETURN)   -> returnStmt()

            match(Keyword.LOCAL)    -> localStmt()

            else                    -> expressionStmt()
        }

        skipSemicolons()

        return stmt
    }

    private fun block(begin: TokenType, vararg ends: Keyword): Stmt.Block {
        val location = here()

        mustSkip(begin)

        skipSemicolons()

        val stmts = mutableListOf<Stmt>()

        while (!matchAny(*ends)) {
            stmts += stmt()
        }

        return Stmt.Block(location, stmts)
    }

    private fun doStmt() =
        block(Keyword.DO, Keyword.END)

    private fun ifStmt(): Stmt.If {
        val location = here()

        mustSkip(Keyword.IF)

        val branches = mutableListOf<Stmt.If.Branch>()

        do {
            val branchLocation = here()

            val test = expr()

            val block = block(Keyword.THEN, Keyword.ELSEIF, Keyword.ELSE, Keyword.END)

            branches += Stmt.If.Branch(branchLocation, test, block)
        }
        while (match(Keyword.ELSEIF))

        val `else` = if (match(Keyword.ELSE))
            block(Keyword.ELSE, Keyword.END)
        else
            Stmt.None

        mustSkip(Keyword.END)

        return Stmt.If(location, branches, `else`)
    }

    private fun whileStmt(): Stmt.While {
        val location = here()

        mustSkip(Keyword.WHILE)

        val test = expr()

        val block = block(Keyword.DO, Keyword.END)

        mustSkip(Keyword.END)

        return Stmt.While(location, test, block)
    }

    private fun repeatStmt(): Stmt.Repeat {
        val location = here()

        val block = block(Keyword.REPEAT, Keyword.UNTIL)

        mustSkip(Keyword.UNTIL)

        val test = expr()

        return Stmt.Repeat(location, test, block)
    }

    private fun forStmt(): Stmt {
        val location = here()

        mustSkip(Keyword.FOR)

        return Stmt.None
    }

    private fun breakStmt(): Stmt.Break {
        val location = here()

        mustSkip(Keyword.BREAK)

        return Stmt.Break(location)
    }

    private fun functionStmt(): Stmt.Function {
        val location = here()

        mustSkip(Keyword.FUNCTION)

        val name = name()

        mustSkip(Symbol.LEFT_PAREN)

        val params = mutableListOf<Expr.Name>()

        if (!match(Symbol.RIGHT_PAREN)) {
            do
                params += name()
            while (skip(Symbol.COMMA))
        }

        val block = block(Symbol.RIGHT_PAREN, Keyword.END)

        mustSkip(Keyword.END)

        return Stmt.Function(location, name, params, block)
    }

    private fun returnStmt(): Stmt.Return {
        val location = here()

        mustSkip(Keyword.RETURN)

        val exprs = mutableListOf<Expr>()

        if (!matchAny(Keyword.END, Keyword.ELSE, Keyword.UNTIL)) {
            do {
                exprs += expr()
            }
            while (skip(Symbol.COMMA))

            if (!matchAny(Keyword.END, Keyword.ELSE, Keyword.UNTIL)) invalidReturn(location)
        }

        return Stmt.Return(location, exprs)
    }

    private fun localStmt(): Stmt.Assign {
        val location = here()

        mustSkip(Keyword.LOCAL)

        val names = mutableListOf<Expr.Name>()

        do {
            names += name()
        }
        while (skip(Symbol.COMMA))

        mustSkip(Symbol.EQUAL)

        val exprs = mutableListOf<Expr>()

        do {
            exprs += expr()
        }
        while (skip(Symbol.COMMA))

        return Stmt.Assign(location, true, names, exprs)
    }

    private fun expressionStmt(): Stmt {
        val location = here()

        val expr = expr()

        return if (matchAny(Symbol.COMMA, Symbol.EQUAL) && expr is Expr.Name) {
            val names = mutableListOf(expr)

            if (!skip(Symbol.EQUAL)) {
                while (skip(Symbol.COMMA)) {
                    names += name()
                }

                mustSkip(Symbol.EQUAL)
            }

            val exprs = mutableListOf<Expr>()

            do {
                exprs += expr()
            }
            while (skip(Symbol.COMMA))

            Stmt.Assign(location, false, names, exprs)
        }
        else Stmt.Expression(location, expr)
    }

    private fun expr() =
        logicalOr()

    private fun logicalOr(): Expr {
        var expr = logicalAnd()

        while (matchAny(Keyword.OR)) {
            val op = peek()

            mustSkip(op.type)

            expr = Expr.Binary(op.context, Expr.Binary.Operator[op.type], expr, logicalAnd())
        }

        return expr
    }

    private fun logicalAnd(): Expr {
        var expr = equality()

        while (matchAny(Keyword.AND)) {
            val op = peek()

            mustSkip(op.type)

            expr = Expr.Binary(op.context, Expr.Binary.Operator[op.type], expr, equality())
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = relational()

        while (matchAny(Symbol.DOUBLE_EQUAL, Symbol.TILDE_EQUAL)) {
            val op = peek()

            mustSkip(op.type)

            expr = Expr.Binary(op.context, Expr.Binary.Operator[op.type], expr, relational())
        }

        return expr
    }

    private fun relational(): Expr {
        var expr = concat()

        while (matchAny(Symbol.LESS, Symbol.LESS_EQUAL, Symbol.GREATER, Symbol.GREATER_EQUAL)) {
            val op = peek()

            mustSkip(op.type)

            expr = Expr.Binary(op.context, Expr.Binary.Operator[op.type], expr, concat())
        }

        return expr
    }

    private fun concat(): Expr {
        var expr = additive()

        while (matchAny(Symbol.DOUBLE_DOT)) {
            val op = peek()

            mustSkip(op.type)

            expr = Expr.Binary(op.context, Expr.Binary.Operator[op.type], expr, concat())
        }

        return expr
    }

    private fun additive(): Expr {
        var expr = multiplicative()

        while (matchAny(Symbol.PLUS, Symbol.DASH)) {
            val op = peek()

            mustSkip(op.type)

            expr = Expr.Binary(op.context, Expr.Binary.Operator[op.type], expr, multiplicative())
        }

        return expr
    }

    private fun multiplicative(): Expr {
        var expr = prefix()

        while (matchAny(Symbol.STAR, Symbol.SLASH, Symbol.DOUBLE_SLASH, Symbol.PERCENT)) {
            val op = peek()

            mustSkip(op.type)

            expr = Expr.Binary(op.context, Expr.Binary.Operator[op.type], expr, prefix())
        }

        return expr
    }

    private fun prefix(): Expr {
        return if (matchAny(Symbol.DASH, Keyword.NOT, Symbol.POUND)) {
            val op = peek()

            mustSkip(op.type)

            Expr.Unary(op.context, Expr.Unary.Operator[op.type], prefix())
        }
        else {
            exponential()
        }
    }

    private fun exponential(): Expr {
        var expr = postfix()

        while (matchAny(Symbol.CARET)) {
            val op = peek()

            mustSkip(op.type)

            expr = Expr.Binary(op.context, Expr.Binary.Operator[op.type], expr, exponential())
        }

        return expr
    }

    private fun postfix(): Expr {
        var expr = terminal()

        while (matchAny(Symbol.DOT, Symbol.LEFT_SQUARE, Symbol.LEFT_PAREN, Symbol.LEFT_BRACE) || match<Value<*>>()) {
            val op = peek()

            expr = when {
                skip(Symbol.DOT)         -> {
                    val member = name().toValue()

                    if (skip(Symbol.EQUAL))
                        Expr.SetIndex(op.context, expr, member, expr())
                    else
                        Expr.GetIndex(op.context, expr, member)
                }

                skip(Symbol.LEFT_SQUARE) -> {
                    val index = expr()

                    mustSkip(Symbol.RIGHT_SQUARE)

                    if (skip(Symbol.EQUAL))
                        Expr.SetIndex(op.context, expr, index, expr())
                    else
                        Expr.GetIndex(op.context, expr, index)
                }

                skip(Symbol.LEFT_PAREN)  -> {
                    val args = mutableListOf<Expr>()

                    if (!skip(Symbol.RIGHT_PAREN)) {
                        do {
                            args += expr()
                        }
                        while (skip(Symbol.COMMA))

                        mustSkip(Symbol.RIGHT_PAREN)
                    }

                    Expr.Invoke(op.context, expr, args)
                }

                match(Symbol.LEFT_BRACE) -> {
                    val arg = table()

                    Expr.Invoke(op.context, expr, listOf(arg))
                }

                match<Value<*>>()        -> Expr.Invoke(op.context, expr, listOf(value()))

                else                     -> failure("Broken postfix operator")
            }
        }

        return expr
    }

    private fun terminal() = when {
        match<Value<*>>()        -> value()

        match<Name>()            -> name()

        match(Symbol.LEFT_BRACE) -> table()

        match(Keyword.FUNCTION)  -> lambda()

        match(Symbol.LEFT_PAREN) -> nested()

        else                     -> invalidTerminal(peek().type, peek().context)
    }

    private fun value(): Expr.Value {
        val (context, type) = get<Value<*>>()

        return Expr.Value(context, type.value)
    }

    private fun name(): Expr.Name {
        val (context, type) = get<Name>()

        return Expr.Name(context, type.value)
    }

    private fun table(): Expr.Table {
        val location = here()

        mustSkip(Symbol.LEFT_BRACE)

        val listInit = mutableListOf<Expr>()
        val mapInit = mutableMapOf<String, Expr>()

        if (!skip(Symbol.RIGHT_BRACE)) {
            do {
                val expr = expr()

                if (skip(Symbol.EQUAL)) {
                    if (expr is Expr.Name)
                        mapInit[expr.name] = expr()
                    else
                        invalidTableKey(expr.context)
                }
                else
                    listInit.add(expr)
            }
            while (skip(Symbol.COMMA))

            mustSkip(Symbol.RIGHT_BRACE)
        }

        return Expr.Table(location, listInit, mapInit)
    }

    private fun lambda(): Expr.Lambda {
        val location = here()

        mustSkip(Keyword.FUNCTION)

        mustSkip(Symbol.LEFT_PAREN)

        val params = mutableListOf<Expr.Name>()

        if (!match(Symbol.RIGHT_PAREN))
            do
                params += name()
            while (skip(Symbol.COMMA))

        val block = block(Symbol.RIGHT_PAREN, Keyword.END)

        mustSkip(Keyword.END)

        return Expr.Lambda(location, params, block)
    }

    private fun nested(): Expr {
        mustSkip(Symbol.LEFT_PAREN)

        val expr = expr()

        mustSkip(Symbol.RIGHT_PAREN)

        return expr
    }
}