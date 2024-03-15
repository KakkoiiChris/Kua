package kua.util

import java.io.File
import java.io.FileWriter
import java.sql.Timestamp

object Console {
    private var output: FileWriter? = null
    
    fun startLog(file: File) {
        val writer = FileWriter(file, true)
        
        writer.write("LOG START: ${getTimeStamp()}")
        writer.appendLine()
        writer.appendLine()
        
        output = writer
    }
    
    fun stopLog() {
        output?.write("LOG STOP: ${getTimeStamp()}")
        output?.appendLine()
        output?.appendLine()
        
        val comments = mutableListOf<String>()
        
        var get = true
        
        do {
            print("[Log Comments] >")
            
            val comment = kotlin.io.readLine() ?: ""
            
            if (comment.isEmpty()) {
                get = false
            }
            else {
                comments += comment
            }
        }
        while (get)
        
        if (comments.isNotEmpty()) {
            output?.write("COMMENTS:")
            output?.appendLine()
            comments.forEach {
                output?.write("- ")
                output?.write(it)
                output?.appendLine()
            }
            output?.appendLine()
        }
        else {
            output?.appendLine()
            output?.appendLine()
        }
        
        output?.close()
        
        output = null
    }
    
    private fun getTimeStamp() =
        Timestamp(System.currentTimeMillis()).toString()
    
    fun cls() =
        ProcessBuilder("cmd", "/c", "cls")
            .inheritIO()
            .start()
            .waitFor()
    
    fun write(x: Any) {
        val s = x.truncate().toString()
        
        print(s)
        
        output?.write(s)
    }
    
    fun writeLine(x: Any) {
        val s = x.truncate().toString()
        
        println(s)
        
        output?.write(s)
        output?.appendLine()
    }
    
    fun newLine() {
        println()
        
        output?.appendLine()
    }
    
    fun error(x: Any) {
        val s = x.truncate().toString()
        
        System.err.print(s)
        
        output?.write(s)
    }
    
    fun errorLine(x: Any) {
        val s = x.truncate().toString()
        
        System.err.println(s)
        
        output?.write(s)
        output?.appendLine()
    }
    
    fun readLine(): String {
        val input = kotlin.io.readLine() ?: ""
        
        output?.write(input)
        output?.appendLine()
        
        return input
    }
}

fun Any.truncate() =
    if (this is Double && this == this.toInt())
        this.toInt()
    else
        this