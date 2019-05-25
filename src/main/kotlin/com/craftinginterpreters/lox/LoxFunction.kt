package com.craftinginterpreters.lox

class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean
): LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val env = Environment(closure)
        for ((param, argument) in declaration.params.zip(arguments)) {
            env.define(param.lexeme, argument)
        }

        try {
            interpreter.executeBlock(declaration.body, env)
        } catch (returnValue: Return) {
            if (isInitializer) {
                return closure.getAt(0, "this")
            }
            return returnValue.value
        }
        if (isInitializer) {
            return closure.getAt(0, "this")
        }

        return null
    }

    override fun arity(): Int {
        return declaration.params.size
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }

    fun bind(loxInstance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", loxInstance)
        return LoxFunction(declaration, environment, isInitializer)
    }
}
