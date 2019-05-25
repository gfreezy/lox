package com.craftinginterpreters.lox

abstract class Expr {
    abstract fun <R> accept(visitor: Visitor<R>): R

    data class Assign(val name: Token, val value: Expr): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitAssignExpr(this)
        }
    }

    data class Binary(val left: Expr, val operator: Token, val right: Expr): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitBinaryExpr(this)
        }
    }

    data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitCallExpr(this)
        }
    }

    data class Get(val obj: Expr, val name: Token): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitGetExpr(this)
        }
    }

    data class Grouping(val expression: Expr): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitGroupingExpr(this)
        }
    }

    data class Literal(val value: Any?): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitLiteralExpr(this)
        }
    }

    data class Logical(val left: Expr, val operator: Token, val right: Expr): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitLogicalExpr(this)
        }
    }

    data class Set(val obj: Expr, val name: Token, val value: Expr): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitSetExpr(this)
        }
    }

    data class Super(val keyword: Token, val method: Token): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitSuperExpr(this)
        }
    }

    data class This(val keyword: Token): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitThisExpr(this)
        }
    }

    data class Unary(val operator: Token, val right: Expr): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitUnaryExpr(this)
        }
    }

    data class Variable(val name: Token): Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitVariableExpr(this)
        }
    }

    interface Visitor<T> {
        fun visitAssignExpr(expr: Assign): T
        fun visitBinaryExpr(expr: Binary): T
        fun visitCallExpr(expr: Call): T
        fun visitGetExpr(expr: Get): T
        fun visitGroupingExpr(expr: Grouping): T
        fun visitLiteralExpr(expr: Literal): T
        fun visitLogicalExpr(expr: Logical): T
        fun visitSetExpr(expr: Set): T
        fun visitSuperExpr(expr: Super): T
        fun visitThisExpr(expr: This): T
        fun visitUnaryExpr(expr: Unary): T
        fun visitVariableExpr(expr: Variable): T
    }
}
