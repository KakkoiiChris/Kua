package kua

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

data class Source(val name: String, val text: String) {
    companion object {
        fun of(path: Path) =
            Source(path.name, Files.readString(path))
    }
}