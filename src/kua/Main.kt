package kua

import kua.lexer.Lexer
import kua.parser.Parser
import kua.script.Script
import java.io.File

fun main(args: Array<String>) {
    when (args.size) {
        0 -> TODO("REPL")
        
        1 -> runFile(args[0])
        
        else-> error("Usage: lua [fileName]")
    }
}

private fun runFile(fileName: String) {
    try {
        val file = File(fileName)
        
        val source = file.readText()
        
        val lexer = Lexer(source)
        
        val parser = Parser(lexer)
        
        val chunk = parser.parse()
        
        val script = Script(chunk)
        
        val s = System.nanoTime()
        
        val result = script.run()
        
        val e = System.nanoTime()
        
        println("Done: $result (${(e - s) / 1.0e6} ms)\n")
    }
    catch (e: Exception) {
        e.printStackTrace()
        Thread.sleep(10)
    }
}