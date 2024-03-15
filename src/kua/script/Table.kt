/*      ___       ___           ___
 *     /\__\     /\__\         /\  \
 *    /:/  /    /:/  /        /::\  \
 *   /:/  /    /:/  /        /:/\:\  \
 *  /:/  /    /:/  /  ___   /::\~\:\  \
 * /:/__/    /:/__/  /\__\ /:/\:\ \:\__\
 * \:\  \    \:\  \ /:/  / \/__\:\/:/  /
 *  \:\  \    \:\  /:/  /       \::/  /
 *   \:\  \    \:\/:/  /        /:/  /
 *    \:\__\    \::/  /        /:/  /
 *     \/__/     \/__/         \/__/
 *          Lua 5.4 Interpreter
 *         Kotlin Implementation
 *          Christian Alexander
 */
package kua.script

import kua.parser.Expr

class Table {
    private val list = mutableListOf<Any>()
    private val map = mutableMapOf<String, Any>()
    
    val length get() = list.indexOfLast { it !is Nil } + 1
    
    operator fun get(index: Double) =
        if (index.toInt() - 1 in list.indices) {
            list[index.toInt() - 1]
        }
        else {
            while (index.toInt() != list.size) {
                list += Nil
            }
            
            list[index.toInt() - 1]
        }
    
    operator fun plusAssign(value: Any) {
        list += value
    }
    
    operator fun get(name: Expr.Name) =
        get(name.name)
    
    operator fun get(name: String) =
        if (map[name] != null) {
            map[name]!!
        }
        else {
            val symbol = Nil
            map[name] = symbol
            symbol
        }
    
    operator fun set(name: String, value: Any) {
        map[name] = value
    }
    
    override fun toString() = buildString {
        append("{ ")
        
        if (list.isNotEmpty()) {
            append(list.joinToString())
        }
        
        if (map.isNotEmpty()) {
            if (list.isNotEmpty()) {
                append(", ")
            }
            
            append(map.map { (k, v) -> "$k : $v" }.joinToString())
        }
        
        if (list.isNotEmpty() || map.isNotEmpty()) {
            append(" ")
        }
        
        append("}")
    }
}