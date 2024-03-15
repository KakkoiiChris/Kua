package kua.parser

import kua.lexer.Lexer
import kua.lexer.Token
import kua.lexer.Token.Type.*
import kua.lexer.Token.Type.Function
import kua.util.*

class Parser(private val lexer: Lexer) {
    private var currentToken = lexer.next()
    
    fun parse(): Chunk {
        val stmts = mutableListOf<Stmt>()
        
        while (!skip(EndOfFile))
            stmts += stmt()
        
        return Chunk(stmts)
    }
    
    private fun peek() = currentToken
    
    private fun step() {
        if (lexer.hasNext())
            currentToken = lexer.next()
    }
    
    private fun match(type: Token.Type) =
        peek().type == type
    
    private fun matchAny(vararg types: Token.Type): Boolean {
        for (type in types)
            if (match(type))
                return true
        
        return false
    }
    
    private fun skip(type: Token.Type) =
        if (match(type)) {
            step()
            
            true
        }
        else false
    
    private fun mustSkip(type: Token.Type) {
        if (!skip(type))
            invalidType(type, peek().type, peek().location)
    }
    
    private fun skipSemicolons() {
        while (skip(Semicolon)) Unit
    }
    
    private fun here() =
        peek().location
    
    private fun stmt(): Stmt {
        val stmt = when {
            match(Do)       -> doStmt()
            
            match(If)       -> ifStmt()
            
            match(While)    -> whileStmt()
            
            match(Repeat)   -> repeatStmt()
            
            match(For)      -> forStmt()
            
            match(Break)    -> breakStmt()
            
            match(Function) -> functionStmt()
            
            match(Return)   -> returnStmt()
            
            match(Local)    -> localStmt()
            
            else            -> expressionStmt()
        }
        
        skipSemicolons()
        
        return stmt
    }
    
    private fun block(begin: Token.Type, vararg ends: Token.Type): Stmt.Block {
        val location = here()
        
        mustSkip(begin)
        
        skipSemicolons()
        
        val stmts = mutableListOf<Stmt>()
        
        while (!matchAny(*ends))
            stmts += stmt()
        
        return Stmt.Block(location, stmts)
    }
    
    private fun doStmt() =
        block(Do, End)
    
    private fun ifStmt(): Stmt.If {
        val location = here()
        
        mustSkip(If)
        
        val branches = mutableListOf<Stmt.If.Branch>()
        
        do {
            val branchLocation = here()
            
            val test = expr()
            
            val block = block(Then, ElseIf, Else, End)
            
            branches += Stmt.If.Branch(branchLocation, test, block)
        }
        while (match(ElseIf))
        
        val elze = if (match(Else))
            block(Else, End)
        else
            Stmt.None
        
        mustSkip(End)
        
        return Stmt.If(location, branches, elze)
    }
    
    private fun whileStmt(): Stmt.While {
        val location = here()
        
        mustSkip(While)
        
        val test = expr()
        
        val block = block(Do, End)
        
        mustSkip(End)
        
        return Stmt.While(location, test, block)
    }
    
    private fun repeatStmt(): Stmt.Repeat {
        val location = here()
        
        val block = block(Repeat, Until)
        
        mustSkip(Until)
        
        val test = expr()
        
        return Stmt.Repeat(location, test, block)
    }
    
    private fun forStmt(): Stmt {
        val location = here()
        
        mustSkip(For)
        
        return Stmt.None
    }
    
    private fun breakStmt(): Stmt.Break {
        val location = here()
        
        mustSkip(Break)
        
        return Stmt.Break(location)
    }
    
    private fun functionStmt(): Stmt.Function {
        val location = here()
        
        mustSkip(Function)
        
        val name = name()
        
        mustSkip(LeftParen)
        
        val params = mutableListOf<Expr.Name>()
        
        if (!match(RightParen)) {
            do
                params += name()
            while (skip(Comma))
        }
        
        val block = block(RightParen, End)
        
        mustSkip(End)
        
        return Stmt.Function(location, name, params, block)
    }
    
    private fun returnStmt(): Stmt.Return {
        val location = here()
        
        mustSkip(Return)
        
        val exprs = mutableListOf<Expr>()
        
        if (!matchAny(End, Else, Until)) {
            do
                exprs += expr()
            while (skip(Comma))
            
            if (!matchAny(End, Else, Until))
                invalidReturn(location)
        }
        
        if (exprs.isEmpty())
            exprs += Expr.None
        
        return Stmt.Return(location, exprs)
    }
    
    private fun localStmt(): Stmt.Assign {
        val location = here()
        
        mustSkip(Local)
        
        val names = mutableListOf<Expr.Name>()
        
        do
            names += name()
        while (skip(Comma))
        
        mustSkip(Equal)
        
        val exprs = mutableListOf<Expr>()
        
        do
            exprs += expr()
        while (skip(Comma))
        
        return Stmt.Assign(location, true, names, exprs)
    }
    
    private fun expressionStmt(): Stmt {
        val location = here()
        
        val expr = expr()
        
        return if (matchAny(Comma, Equal) && expr is Expr.Name) {
            val names = mutableListOf(expr)
            
            if (!skip(Equal)) {
                while (skip(Comma))
                    names += name()
                
                mustSkip(Equal)
            }
            
            val exprs = mutableListOf<Expr>()
            
            do
                exprs += expr()
            while (skip(Comma))
            
            Stmt.Assign(location, false, names, exprs)
        }
        else Stmt.Expression(location, expr)
    }
    
    private fun expr() =
        logicalOr()
    
    private fun logicalOr(): Expr {
        var expr = logicalAnd()
        
        while (matchAny(Or)) {
            val op = peek()
            
            mustSkip(op.type)
            
            expr = Expr.Binary(op.location, Expr.Binary.Operator[op.type], expr, logicalAnd())
        }
        
        return expr
    }
    
    private fun logicalAnd(): Expr {
        var expr = equality()
        
        while (matchAny(And)) {
            val op = peek()
            
            mustSkip(op.type)
            
            expr = Expr.Binary(op.location, Expr.Binary.Operator[op.type], expr, equality())
        }
        
        return expr
    }
    
    private fun equality(): Expr {
        var expr = relational()
        
        while (matchAny(DoubleEqual, TildeEqual)) {
            val op = peek()
            
            mustSkip(op.type)
            
            expr = Expr.Binary(op.location, Expr.Binary.Operator[op.type], expr, relational())
        }
        
        return expr
    }
    
    private fun relational(): Expr {
        var expr = concat()
        
        while (matchAny(Less, LessEqual, Greater, GreaterEqual)) {
            val op = peek()
            
            mustSkip(op.type)
            
            expr = Expr.Binary(op.location, Expr.Binary.Operator[op.type], expr, concat())
        }
        
        return expr
    }
    
    private fun concat(): Expr {
        var expr = additive()
        
        while (matchAny(DoubleDot)) {
            val op = peek()
            
            mustSkip(op.type)
            
            expr = Expr.Binary(op.location, Expr.Binary.Operator[op.type], expr, concat())
        }
        
        return expr
    }
    
    private fun additive(): Expr {
        var expr = multiplicative()
        
        while (matchAny(Plus, Dash)) {
            val op = peek()
            
            mustSkip(op.type)
            
            expr = Expr.Binary(op.location, Expr.Binary.Operator[op.type], expr, multiplicative())
        }
        
        return expr
    }
    
    private fun multiplicative(): Expr {
        var expr = prefix()
        
        while (matchAny(Star, Slash, Percent)) {
            val op = peek()
            
            mustSkip(op.type)
            
            expr = Expr.Binary(op.location, Expr.Binary.Operator[op.type], expr, prefix())
        }
        
        return expr
    }
    
    private fun prefix(): Expr {
        return if (matchAny(Dash, Not, Pound)) {
            val op = peek()
            
            mustSkip(op.type)
            
            Expr.Unary(op.location, Expr.Unary.Operator[op.type], prefix())
        }
        else {
            exponential()
        }
    }
    
    private fun exponential(): Expr {
        var expr = postfix()
        
        while (matchAny(Caret)) {
            val op = peek()
            
            mustSkip(op.type)
            
            expr = Expr.Binary(op.location, Expr.Binary.Operator[op.type], expr, exponential())
        }
        
        return expr
    }
    
    private fun postfix(): Expr {
        var expr = terminal()
        
        while (matchAny(Dot, LeftSquare, LeftParen, LeftBrace, Value)) {
            val op = peek()
            
            expr = when {
                skip(Dot)        -> {
                    val member = name()
                    
                    if (skip(Equal))
                        Expr.SetMember(op.location, expr, member, expr())
                    else
                        Expr.GetMember(op.location, expr, member)
                }
                
                skip(LeftSquare) -> {
                    val index = expr()
                    
                    mustSkip(RightSquare)
                    
                    if (skip(Equal))
                        Expr.SetIndex(op.location, expr, index, expr())
                    else
                        Expr.GetIndex(op.location, expr, index)
                }
                
                skip(LeftParen)  -> {
                    val args = mutableListOf<Expr>()
                    
                    if (!skip(RightParen)) {
                        do
                            args += expr()
                        while (skip(Comma))
                        
                        mustSkip(RightParen)
                    }
                    
                    Expr.Invoke(op.location, expr, args)
                }
                
                match(LeftBrace) -> {
                    val arg = table()
                    
                    Expr.Invoke(op.location, expr, listOf(arg))
                }
                
                match(Value)     -> Expr.Invoke(op.location, expr, listOf(value()))
                
                else             -> failure("Broken postfix operator")
            }
        }
        
        return expr
    }
    
    private fun terminal() = when {
        match(Value)     -> value()
        
        match(Name)      -> name()
        
        match(LeftBrace) -> table()
        
        match(Function)  -> lambda()
        
        match(LeftParen) -> nested()
        
        else             -> invalidTerminal(peek().type, peek().location)
    }
    
    private fun value(): Expr.Value {
        val token = peek()
        
        mustSkip(Value)
        
        return Expr.Value(token.location, token.value)
    }
    
    private fun name(): Expr.Name {
        val token = peek()
        
        mustSkip(Name)
        
        return Expr.Name(token.location, token.value as String)
    }
    
    private fun table(): Expr.Table {
        val location = here()
        
        mustSkip(LeftBrace)
        
        val listInit = mutableListOf<Expr>()
        val mapInit = mutableMapOf<String, Expr>()
        
        if (!skip(RightBrace)) {
            do {
                val expr = expr()
                
                if (skip(Equal)) {
                    if (expr is Expr.Name)
                        mapInit[expr.name] = expr()
                    else
                        invalidTableKey(expr.location)
                }
                else
                    listInit.add(expr)
            }
            while (skip(Comma))
            
            mustSkip(RightBrace)
        }
        
        return Expr.Table(location, listInit, mapInit)
    }
    
    private fun lambda(): Expr.Lambda {
        val location = here()
        
        mustSkip(Function)
        
        mustSkip(LeftParen)
        
        val params = mutableListOf<Expr.Name>()
        
        if (!match(RightParen))
            do
                params += name()
            while (skip(Comma))
        
        val block = block(RightParen, End)
        
        mustSkip(End)
        
        return Expr.Lambda(location, params, block)
    }
    
    private fun nested(): Expr {
        mustSkip(LeftParen)
        
        val expr = expr()
        
        mustSkip(RightParen)
        
        return expr
    }
}