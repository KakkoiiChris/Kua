package kua.util

open class Stack<T> {
    val elements = mutableListOf<T>()

    val count get() = elements.size

    fun isEmpty() = elements.isEmpty()
    
    fun isNotEmpty() = elements.isNotEmpty()

    fun push(item: T) = elements.add(item)

    fun pop(): T? = if (isEmpty())
        null
    else
        elements.removeAt(count - 1)

    fun peek(): T? = elements.lastOrNull()

    fun clear() = elements.clear()

    override fun toString(): String = elements.toString()
}

fun <T> Stack<T>.push(items: Collection<T>) = items.forEach { this.push(it) }