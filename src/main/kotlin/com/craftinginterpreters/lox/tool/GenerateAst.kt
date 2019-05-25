package com.craftinginterpreters.lox.tool

import java.io.PrintWriter

object GenerateAst {

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 1) {
            System.err.println("Usage: generatea_ast <output directory>")
            System.exit(1)
        }

        val outputDir = args[0]
        defineAst(
            outputDir,
            "Expr",
            arrayOf(
                "Assign" to "val name: Token, val value: Expr",
                "Binary" to "val left: Expr, val operator: Token, val right: Expr",
                "Call" to "val callee: Expr, val paren: Token, val arguments: List<Expr>",
                "Get" to "val obj: Expr, val name: Token",
                "Grouping" to "val expression: Expr",
                "Literal" to "val value: Any?",
                "Logical" to "val left: Expr, val operator: Token, val right: Expr",
                "Set" to "val obj: Expr, val name: Token, val value: Expr",
                "Super" to "val keyword: Token, val method: Token",
                "This" to "val keyword: Token",
                "Unary" to "val operator: Token, val right: Expr",
                "Variable" to "val name: Token"
            )
        )

        defineAst(
            outputDir,
            "Stmt",
            arrayOf(
                "Block" to "val statements: List<Stmt>",
                "Class" to "val name: Token, val superclass: Expr.Variable?, val methods: List<Function>",
                "Expression" to "val expression: Expr",
                "Function" to "val name: Token, val params: List<Token>, val body: List<Stmt>, val isClassFunction: Boolean",
                "If" to "val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?",
                "While" to "val condition: Expr, val body: Stmt",
                "Print" to "val expression: Expr",
                "Return" to "val keyword: Token, val value: Expr?",
                "Var" to "val name: Token, val initializer: Expr?"
            )
        )
    }

    private fun defineAst(
        outputDir: String,
        baseName: String,
        types: Array<Pair<String, String>>
    ) {
        val path = "$outputDir/$baseName.kt"
        val writer = PrintWriter(path, "UTF-8")
        writer.println(
            """
         |package com.craftinginterpreters.lox
         |
         |abstract class $baseName {
         |    abstract fun <R> accept(visitor: Visitor<R>): R
         |
       """.trimMargin()
        )

        for (ty in types) {
            val className = ty.first.trim()
            val fields = ty.second.trim()
            defineType(writer, baseName, className, fields)
        }

        defineVisitor(writer, baseName, types)
        writer.println("}")

        writer.close()
    }

    private fun defineType(
        writer: PrintWriter,
        baseName: String,
        className: String,
        fields: String
    ) {
        writer.println(
            """    data class $className($fields): $baseName() {
            |        override fun <T> accept(visitor: Visitor<T>): T {
            |            return visitor.visit$className$baseName(this)
            |        }
            |    }
            |""".trimMargin()
        )
    }

    private fun defineVisitor(
        writer: PrintWriter,
        baseName: String,
        types: Array<Pair<String, String>>
    ) {
        writer.println("    interface Visitor<T> {")
        for (ty in types) {
            val typeName = ty.first
            writer.println(
                "        fun visit$typeName$baseName(${baseName.toLowerCase()}: $typeName): T"
            )
        }
        writer.println("    }")
    }
}
