package kua.util

import kua.lexer.Context
import kua.lexer.Token
import kua.lexer.TokenType
import kua.parser.Expr
import kua.parser.Stmt

class LuaError(
    private val stage: String,
    private val subMessage: String,
    private val context: Context = Context.none,
) : RuntimeException() {
    private val traces = mutableListOf<Stmt.Trace>()
    
    operator fun plusAssign(stmt: Stmt) {
        traces += stmt.trace
    }
    
    override fun toString() = buildString {
        appendLine("Lua $stage Error: $subMessage!$context")
        
        val maxWidth = traces.map { it.qualifier.length }.maxOrNull() ?: 0
        
        for (trace in traces) {
            appendLine("\t${trace.qualifier.padEnd(maxWidth)}$context")
        }
    }
}

private fun generalError(subMessage: String, context: Context): Nothing =
    throw LuaError("General", subMessage, context)

fun failure(subMessage: String): Nothing =
    generalError(subMessage, Context.none)

private fun lexerError(subMessage: String, context: Context): Nothing =
    throw LuaError("Lexer", subMessage, context)

fun illegalCharacter(char: Char, context: Context): Nothing =
    lexerError("Character '$char' is illegal", context)

fun invalidEscape(char: Char, context: Context): Nothing =
    lexerError("Escaped character '$char' is invalid", context)

fun illegalString(string: String, context: Context): Nothing =
    lexerError("Character sequence '$string' is illegal", context)

fun invalidNumber(number: String, context: Context): Nothing =
    lexerError("Number '$number' is invalid", context)

private fun parserError(subMessage: String, context: Context): Nothing =
    throw LuaError("Parser", subMessage, context)

fun invalidReturn(context: Context): Nothing =
    parserError("Return must be the last statement in a block or chunk", context)

fun invalidSingleArgument(arg: Any, context: Context): Nothing =
    parserError("Single argument '$arg' is invalid; must be string or table", context)

fun invalidTableKey(context: Context): Nothing =
    parserError("Table key is invalid; must be a valid name", context)

fun invalidTerminal(type: TokenType, context: Context): Nothing =
    parserError("Terminal beginning with '$type' is invalid", context)

fun invalidType(type: TokenType, expected: TokenType, context: Context): Nothing =
    parserError("Token type '$type' is invalid; expected '$expected'", context)

private fun scriptError(subMessage: String, context: Context): Nothing =
    throw LuaError("Script", subMessage, context)

fun invalidStringIndex(invalid: Any, context: Context): Nothing =
    scriptError("Index '$invalid' for string is invalid; must be a number", context)

fun invalidTableIndex(invalid: Any, context: Context): Nothing =
    scriptError("Index '$invalid' for table is invalid; must be a number or string", context)

fun invalidLeftOperand(left: Any, operator: Expr.Binary.Operator, context: Context): Nothing =
    scriptError("Left operand '$left' for '$operator' operator is invalid!", context)

fun invalidRightOperand(right: Any, operator: Expr.Binary.Operator, context: Context): Nothing =
    scriptError("Right operand '$right' for '$operator' operator is invalid!", context)

fun invalidUnaryOperand(expr: Any, operator: Expr.Unary.Operator, context: Context): Nothing =
    scriptError("Operand '$expr' for '$operator' operator is invalid!", context)

fun nonAccessedValue(value: Any, context: Context): Nothing =
    scriptError("Value '$value' cannot be accessed", context)

fun nonIndexedValue(value: Any, context: Context): Nothing =
    scriptError("Value '$value' cannot be indexed", context)

fun numberCoercion(coerced: Any, context: Context): Nothing =
    scriptError("Value '$coerced' cannot be coerced to a number", context)

fun unassignableLValue(lValue: Any, context: Context): Nothing =
    scriptError("LValue '$lValue' is unassignable", context)