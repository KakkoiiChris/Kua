package kua

import kua.parser.Stmt

sealed interface KuaValue<X> {
    val value: X
}

data object Nil : KuaValue<Nil> {
    override val value = this
}

data class KuaBoolean(override val value: Boolean) : KuaValue<Boolean>

data class KuaNumber(override val value: Double) : KuaValue<Double>

data class KuaString(override val value: String) : KuaValue<String>

data class KuaFunction(override val value: Stmt.Function) : KuaValue<Stmt.Function>

data class KuaUserData(override val value: Double) : KuaValue<Double>

data class KuaThread(override val value: Double) : KuaValue<Double>

data class KuaTable(override val value: kua.runtime.Table) : KuaValue<kua.runtime.Table>

data class KuaTuple(override val value: List<KuaValue<*>>) : KuaValue<List<KuaValue<*>>>
