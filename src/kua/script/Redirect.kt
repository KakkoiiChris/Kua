package kua.script

object Break : Throwable("break")

class Return(val value: Any) : Throwable("return") {
    class Tuple(val values: List<Any>)
}