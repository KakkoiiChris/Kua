package kua.script

import kua.parser.Expr

class Memory {
    private val global = Scope()
    
    private var stack = global
    
    fun peek() =
        stack
    
    fun push(scope: Scope = Scope(stack)) {
        stack = scope
    }
    
    fun pop(): Scope {
        val scope = stack
        
        stack = scope.parent ?: stack
        
        return stack
    }
    
    operator fun get(name: Expr.Name): Any {
        var here: Scope? = stack
        
        while (here != null) {
            val ref = here[name.name]
            
            if (ref != null) {
                return ref
            }
            
            here = here.parent
        }
        
        return global[name.name] ?: Nil
    }
    
    operator fun set(name: Expr.Name, local: Boolean, value: Any) {
        var here: Scope? = if (local) stack else global
        
        while (here != null) {
            val ref = here[name.name]
            
            if (ref != null) {
                if (value == Nil)
                    here.remove(name.name)
                else
                    here[name.name] = value
                
                return
            }
            
            here = here.parent
        }
        
        if (value != Nil)
            global[name.name] = value
    }
    
    class Scope(val parent: Scope? = null) : MutableMap<String, Any> by mutableMapOf()
}