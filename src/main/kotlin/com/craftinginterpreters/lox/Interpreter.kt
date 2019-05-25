package com.craftinginterpreters.lox

class Interpreter: Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    private val locals: MutableMap<Expr, Int> = hashMapOf()
    private val globals: Environment = Environment()
    private var environment = globals

    init {
        globals.define("clock", object: LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                return System.currentTimeMillis() / 1000
            }

            override fun arity(): Int {
                return 0
            }

            override fun toString(): String {
                return "<native fn>"
            }
        })
    }

    override fun visitGetExpr(expr: Expr.Get): Any? {
        val obj = evaluate(expr.obj)
        if (obj is LoxInstance) {
            return obj.get(expr.name)
        }
        throw RuntimeError(expr.name, "Only instance have properties.")
    }

    override fun visitSetExpr(expr: Expr.Set): Any? {
        val obj = evaluate(expr.obj) as? LoxInstance ?: throw RuntimeError(expr.name, "Only instance have fields.")

        val value = evaluate(expr.value)
        obj.set(expr.name, value)
        return value
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value = stmt.value?.let { evaluate(it) }
        throw Return(value)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = LoxFunction(stmt, environment, false)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)

        val arguments: MutableList<Any?> = arrayListOf()
        for (argument in expr.arguments) {
            arguments.add(evaluate(argument))
        }

        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        }

        if (arguments.size != callee.arity()) {
            throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}.")
        }
        return callee.call(this, arguments)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visitSuperExpr(expr: Expr.Super): Any? {
        val distance = locals.getValue(expr)
        val superclass =
            environment.getAt(distance, "super") as? LoxClass ?: throw java.lang.RuntimeException("No super class")

        val obj = environment.getAt(distance - 1, "this") as? LoxInstance
            ?: throw java.lang.RuntimeException("No instance found")
        val method = superclass.findMethod(expr.method.lexeme) ?: throw RuntimeError(
            expr.method,
            "Undefined property '${expr.method.lexeme}'."
        )
        return method.bind(obj)
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)
        if (expr.operator.type == TokenType.Or) {
            if (isTruthy(left)) {
                return left
            }
        } else {
            if (!isTruthy(left)) {
                return left
            }
        }

        return evaluate(expr.right)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(stmt.condition)) {
            execute(stmt.thenBranch)
        } else {
            stmt.elseBranch?.let { execute(it) }
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)

        val distance = locals[expr]
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }

        return value
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val superclass = stmt.superclass?.let {
            evaluate(it) as? LoxClass ?: throw RuntimeError(stmt.superclass.name, "Superclass must be a class")
        }

        environment.define(stmt.name.lexeme, null)

        val methods: MutableMap<String, LoxFunction> = hashMapOf()
        val classMethods: MutableMap<String, LoxFunction> = hashMapOf()
        for (method in stmt.methods) {
            val function = LoxFunction(method, environment, method.name.lexeme == "init")
            if (method.isClassFunction) {
                classMethods[method.name.lexeme] = function
            } else {
                methods[method.name.lexeme] = function
            }
        }
        val metaClass = LoxMetaClass(stmt.name.lexeme, null, classMethods)
        val klass = LoxClass(stmt.name.lexeme, superclass, methods, metaClass = metaClass)
        if (superclass != null) {
            environment = environment.enclosing ?: throw RuntimeException("No enclosing environment")
        }
        environment.assign(stmt.name, klass)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment

            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val value = stmt.initializer?.let { evaluate(it) }
        environment.define(stmt.name.lexeme, value)
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookUpVariable(expr.keyword, expr)
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals.get(name)
        }
    }

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (error: RuntimeError) {
            Lox.runtimeError(error)
        }
    }

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)
        when (expr.operator.type) {
            is TokenType.Minus -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double - right as Double
            }
            is TokenType.Slash -> {
                checkNumberOperands(expr.operator, left, right)
                if (right == 0) {
                    throw RuntimeError(expr.operator, "Divided by zero.")
                }
                return left as Double / right as Double
            }
            is TokenType.Star -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double * right as Double
            }
            is TokenType.Plus -> {
                if (left is Double && right is Double) {
                    return left + right
                }
                if (left is String && right is String) {
                    return left + right
                }
                throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
            }
            is TokenType.Greater -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double > right as Double
            }
            is TokenType.GreaterEqual -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double >= right as Double
            }
            is TokenType.Less -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) < (right as Double)
            }
            is TokenType.LessEqual -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double <= right as Double
            }
            is TokenType.BangEqual -> {
                checkNumberOperands(expr.operator, left, right)
                return !isEqual(left, right)
            }
            is TokenType.EqualEqual -> {
                checkNumberOperands(expr.operator, left, right)
                return isEqual(left, right)
            }
        }
        return null
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)
        when (expr.operator.type) {
            is TokenType.Minus -> {
                checkNumberOperand(expr.operator, right)
                return (right as? Double)?.unaryMinus()
            }
            is TokenType.Bang ->
                return isTruthy(right)
        }
        return null
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun isTruthy(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (obj is Boolean) {
            return obj
        }
        return true
    }

    private fun isEqual(left: Any?, right: Any?): Boolean {
        if (left == null && right == null) {
            return true
        }
        if (left == null) {
            return false
        }
        return left == right
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) {
            return
        }
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return

        throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun stringify(obj: Any?): String {
        if (obj == null) {
            return "nil"
        }

        if (obj is Double) {
            var text = obj.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length - 2)
            }
            return text
        }

        return obj.toString()
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }
}
