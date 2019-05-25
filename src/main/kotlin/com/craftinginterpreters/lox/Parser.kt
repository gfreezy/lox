package com.craftinginterpreters.lox

import kotlin.reflect.KClass


class Parser(private val tokens: List<Token>) {
    private class ParseError: RuntimeException()

    private var current = 0

    private enum class FunctionKind {
        FUNCTION {
            override fun toString(): String {
                return "function"
            }
        },
        METHOD {
            override fun toString(): String {
                return "method"
            }
        },
        CLASS {
            override fun toString(): String {
                return "class function"
            }
        }
    }

    fun parse(): List<Stmt> {
        val statements = arrayListOf<Stmt>()
        while (!isAtEnd()) {
            val stmt = declaration()
            stmt?.let { statements.add(it) }
            println("stmt ${peek()}")
        }
        return statements
    }

    private fun declaration(): Stmt? {
        try {
            if (match(TokenType.Class::class)) {
                return classDeclaration()
            }
            if (match(TokenType.Fun::class)) {
                return function(FunctionKind.FUNCTION)
            }
            if (match(TokenType.Var::class)) {
                return varDeclaration()
            }
            return statement()
        } catch (error: ParseError) {
            println("error")
            synchronize()
            return null
        }
    }

    private fun classDeclaration(): Stmt {
        val name = consume(TokenType.Identifier::class, "Expect class name.")

        val superclass = if (match(TokenType.Less::class)) {
            consume(TokenType.Identifier::class, "Expect superclass name.")
            Expr.Variable(previous())
        } else {
            null
        }

        consume(TokenType.LeftBrace::class, "Expect '{' before class body.")

        val methods = arrayListOf<Stmt.Function>()
        while (!checkType(TokenType.RightBrace::class) && !isAtEnd()) {
            println("methods ${peek()}")
            methods.add(functionInClass())
            println("methods")
        }
        consume(TokenType.RightBrace::class, "Expect '}' after class body.")

        return Stmt.Class(name, superclass, methods)
    }

    private fun functionInClass(): Stmt.Function {
        if (match(TokenType.Fun::class)) {
            return function(FunctionKind.METHOD)
        }

        if (match(TokenType.Class::class) && match(TokenType.Fun::class)) {
            return function(FunctionKind.CLASS)
        }

        throw error(peek(), "Expect 'class fun' or 'fun' in class body.")
    }

    private fun function(kind: FunctionKind): Stmt.Function {
        val name = consume(TokenType.Identifier::class, "Expect $kind name")
        consume(TokenType.LeftParen::class, "Expect '(' after $kind name.")
        val parameters: MutableList<Token> = arrayListOf()
        if (!checkType(TokenType.RightParen::class)) {
            do {
                if (parameters.size >= 8) {
                    error(peek(), "Cannot have more than 8 parameters.")
                }

                parameters.add(consume(TokenType.Identifier::class, "Expect parameter name."))
            } while (match(TokenType.Comma::class))
        }
        consume(TokenType.RightParen::class, "Expect ')' after parameters.")

        consume(TokenType.LeftBrace::class, "Expect '{' before $kind body.")
        val body = block()
        return Stmt.Function(name, parameters, body, kind == FunctionKind.CLASS)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.Identifier::class, "Expect variable name")

        val initializer = if (match(TokenType.Equal::class)) {
            expression()
        } else {
            null
        }
        consume(TokenType.Semicolon::class, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        if (match(TokenType.For::class)) {
            return forStatement()
        }
        if (match(TokenType.If::class)) {
            return ifStatement()
        }
        if (match(TokenType.While::class)) {
            return whileStatement()
        }
        if (match(TokenType.Print::class)) {
            return printStatement()
        }
        if (match(TokenType.Return::class)) {
            return returnStatement()
        }
        if (match(TokenType.LeftBrace::class)) {
            return Stmt.Block(block())
        }
        return expressionStatement()
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!checkType(TokenType.Semicolon::class)) {
            expression()
        } else {
            null
        }
        consume(TokenType.Semicolon::class, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LeftParen::class, "Expect '(' after 'for'.")
        val initializer = when {
            match(TokenType.Semicolon::class) -> null
            match(TokenType.Var::class) -> varDeclaration()
            else -> expressionStatement()
        }

        val condition = if (!checkType(TokenType.Semicolon::class)) {
            expression()
        } else {
            null
        }
        consume(TokenType.Semicolon::class, "Expect ';' after loop condition.")

        val increment = if (!checkType(TokenType.RightParen::class)) {
            expression()
        } else {
            null
        }
        consume(TokenType.RightParen::class, "Expect ')' after for clauses.")

        var body = statement()

        if (increment != null) {
            body = Stmt.Block(
                arrayListOf(
                    body, Stmt.Expression(increment)
                )
            )
        }

        if (condition != null) {
            body = Stmt.While(condition, body)
        }

        if (initializer != null) {
            body = Stmt.Block(arrayListOf(initializer, body))
        }

        return body
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LeftParen::class, "Expect '(' after 'while'")
        val condition = expression()
        consume(TokenType.RightParen::class, "Expect ')' after condition")
        val body = statement()
        return Stmt.While(condition, body)
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.If::class, "Expected '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RightParen::class, "Expect ')' after if condition.")

        val thenBranch = statement()
        val elseBranch = if (match(TokenType.Else::class)) {
            statement()
        } else {
            null
        }

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun block(): List<Stmt> {
        val statements: MutableList<Stmt> = arrayListOf()
        println("peek ${peek()}")
        while (!checkType(TokenType.RightBrace::class) && !isAtEnd()) {
            println("in block ${peek()}")
            declaration()?.let(statements::add)
        }
        consume(TokenType.RightBrace::class, "Expect '}' after block.")
        println("return statments")
        return statements
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.Semicolon::class, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.Semicolon::class, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(TokenType.Equal::class)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }
            if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
            }

            error(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(TokenType.Or::class)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(TokenType.And::class)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BangEqual::class, TokenType.EqualEqual::class)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = addition()

        while (match(
                TokenType.Greater::class,
                TokenType.GreaterEqual::class,
                TokenType.Less::class,
                TokenType.LessEqual::class
            )
        ) {
            val operator = previous()
            val right = addition()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun addition(): Expr {
        var expr = multiplication()

        while (match(TokenType.Minus::class, TokenType.Plus::class)) {
            val operator = previous()
            val right = multiplication()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }


    private fun multiplication(): Expr {
        var expr = unary()

        while (match(TokenType.Slash::class, TokenType.Star::class)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.Bang::class, TokenType.Minus::class)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }
        return call()
    }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            if (match(TokenType.LeftParen::class)) {
                expr = finishCall(expr)
            } else if (match(TokenType.Dot::class)) {
                val name = consume(TokenType.Identifier::class, "Expect property name after '.'.")
                expr = Expr.Get(expr, name)
            } else {
                break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments: MutableList<Expr> = arrayListOf()
        if (!checkType((TokenType.RightParen::class))) {
            do {
                if (arguments.size >= 8) {
                    error(peek(), "Cannot have more than 8 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.Comma::class))
        }

        val paren = consume(TokenType.RightParen::class, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr {
        if (match(TokenType.False::class)) return Expr.Literal(false)
        if (match(TokenType.True::class)) return Expr.Literal(true)
        if (match(TokenType.Nil::class)) return Expr.Literal(null)
        if (match(TokenType.Super::class)) {
            val keyword = previous()
            consume(TokenType.Dot::class, "Expect '.' after 'super'.")
            val method = consume(TokenType.Identifier::class, "Expect superclass method name.")
            return Expr.Super(keyword, method)
        }
        if (match(TokenType.This::class)) return Expr.This(previous())

        if (match(TokenType.Number::class, TokenType.String::class)) {
            @Suppress("IMPLICIT_CAST_TO_ANY") val expr = when (val ty = previous().type) {
                is TokenType.Number -> ty.value
                is TokenType.String -> ty.value
                else -> {
                    throw RuntimeException("Never reachable")
                }
            }

            return Expr.Literal(expr)
        }
        if (match(TokenType.Identifier::class)) {
            return Expr.Variable(previous())
        }

        if (match(TokenType.LeftParen::class)) {
            val expr = expression()
            consume(TokenType.RightParen::class, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }
        throw error(peek(), "Expect expression.")
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == TokenType.Semicolon) {
                return
            }

            when (peek().type) {
                TokenType.Class,
                TokenType.Fun,
                TokenType.For,
                TokenType.If,
                TokenType.Print,
                TokenType.Var,
                TokenType.While,
                TokenType.Return -> return
            }

            advance()
        }
    }

    private fun consume(type: KClass<out TokenType>, message: String): Token {
        if (checkType(type)) {
            return advance()
        }

        throw error(peek(), message)
    }

    private fun match(vararg types: KClass<out TokenType>): Boolean {
        for (type in types) {
            if (checkType(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun advance(): Token {
        if (!isAtEnd()) {
            current++
        }
        return previous()
    }

    private fun checkType(type: KClass<out TokenType>): Boolean {
        if (isAtEnd()) {
            return false
        }
        return peek().type::class == type
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.Eof
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun error(token: Token, message: String): ParseError {
        Lox.error(token, message)
        return ParseError()
    }

}
