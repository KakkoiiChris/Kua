package kua.runtime

import kua.KuaValue

data object Break : Throwable("break")

class Return(val value: KuaValue<*>) : Throwable("return")