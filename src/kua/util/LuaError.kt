package kua.util

import kua.lexer.Location
import kua.lexer.Token
import kua.parser.Expr
import kua.parser.Stmt

class LuaError(
    private val stage: String,
    private val subMessage: String,
    private val location: Location = Location(),
) : RuntimeException() {
    private val traces = mutableListOf<Stmt.Trace>()
    
    operator fun plusAssign(stmt: Stmt) {
        traces += stmt.trace
    }
    
    override fun toString() = buildString {
        appendLine("Lua $stage Error: $subMessage!$location")
        
        val maxWidth = traces.map { it.qualifier.length }.maxOrNull() ?: 0
        
        for (trace in traces) {
            appendLine("\t${trace.qualifier.padEnd(maxWidth)}$location")
        }
    }
}

private fun generalError(subMessage: String, location: Location): Nothing =
    throw LuaError("General", subMessage, location)

fun failure(subMessage: String): Nothing =
    generalError(subMessage, Location())

private fun lexerError(subMessage: String, location: Location): Nothing =
    throw LuaError("Lexer", subMessage, location)

fun illegalCharacter(char: Char, location: Location): Nothing =
    lexerError("Character '$char' is illegal", location)

fun invalidEscape(char: Char, location: Location): Nothing =
    lexerError("Escaped character '$char' is invalid", location)

fun illegalString(string: String, location: Location): Nothing =
    lexerError("Character sequence '$string' is illegal", location)

fun invalidNumber(number: String, location: Location): Nothing =
    lexerError("Number '$number' is invalid", location)

private fun parserError(subMessage: String, location: Location): Nothing =
    throw LuaError("Parser", subMessage, location)

fun invalidReturn(location: Location): Nothing =
    parserError("Return must be the last statement in a block or chunk", location)

fun invalidSingleArgument(arg: Any, location: Location): Nothing =
    parserError("Single argument '$arg' is invalid; must be string or table", location)

fun invalidTableKey(location: Location): Nothing =
    parserError("Table key is invalid; must be a valid name", location)

fun invalidTerminal(type: Token.Type, location: Location): Nothing =
    parserError("Terminal beginning with '$type' is invalid", location)

fun invalidType(type: Token.Type, expected: Token.Type, location: Location): Nothing =
    parserError("Token type '$type' is invalid; expected '$expected'", location)

private fun scriptError(subMessage: String, location: Location): Nothing =
    throw LuaError("Script", subMessage, location)

fun invalidStringIndex(invalid: Any, location: Location): Nothing =
    scriptError("Index '$invalid' for string is invalid; must be a number", location)

fun invalidTableIndex(invalid: Any, location: Location): Nothing =
    scriptError("Index '$invalid' for table is invalid; must be a number or string", location)

fun invalidLeftOperand(left: Any, operator: Expr.Binary.Operator, location: Location): Nothing =
    scriptError("Left operand '$left' for '$operator' operator is invalid!", location)

fun invalidRightOperand(right: Any, operator: Expr.Binary.Operator, location: Location): Nothing =
    scriptError("Right operand '$right' for '$operator' operator is invalid!", location)

fun invalidUnaryOperand(expr: Any, operator: Expr.Unary.Operator, location: Location): Nothing =
    scriptError("Operand '$expr' for '$operator' operator is invalid!", location)

fun nonAccessedValue(value: Any, location: Location): Nothing =
    scriptError("Value '$value' cannot be accessed", location)

fun nonIndexedValue(value: Any, location: Location): Nothing =
    scriptError("Value '$value' cannot be indexed", location)

fun numberCoercion(coerced: Any, location: Location): Nothing =
    scriptError("Value '$coerced' cannot be coerced to a number", location)

fun unassignableLValue(lValue: Any, location: Location): Nothing =
    scriptError("LValue '$lValue' is unassignable", location)