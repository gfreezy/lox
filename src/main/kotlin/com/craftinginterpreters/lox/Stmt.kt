package com.craftinginterpreters.lox

abstract class Stmt {
    abstract fun <R> accept(visitor: Visitor<R>): R

    data class Block(val statements: List<Stmt>): Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitBlockStmt(this)
        }
    }

    data class Class(val name: Token, val superclass: Expr.Variable?, val methods: List<Function>): Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitClassStmt(this)
        }
    }

    data class Expression(val expression: Expr): Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitExpressionStmt(this)
        }
    }

    data class Function(val name: Token, val params: List<Token>, val body: List<Stmt>, val isClassFunction: Boolean):
        Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitFunctionStmt(this)
        }
    }

    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?): Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitIfStmt(this)
        }
    }

    data class While(val condition: Expr, val body: Stmt): Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitWhileStmt(this)
        }
    }

    data class Print(val expression: Expr): Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitPrintStmt(this)
        }
    }

    data class Return(val keyword: Token, val value: Expr?): Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitReturnStmt(this)
        }
    }

    data class Var(val name: Token, val initializer: Expr?): Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitVarStmt(this)
        }
    }

    interface Visitor<T> {
        fun visitBlockStmt(stmt: Block): T
        fun visitClassStmt(stmt: Class): T
        fun visitExpressionStmt(stmt: Expression): T
        fun visitFunctionStmt(stmt: Function): T
        fun visitIfStmt(stmt: If): T
        fun visitWhileStmt(stmt: While): T
        fun visitPrintStmt(stmt: Print): T
        fun visitReturnStmt(stmt: Return): T
        fun visitVarStmt(stmt: Var): T
    }
}
