package com.craftinginterpreters.lox

open class LoxInstance(private val klass: LoxBaseClass) {
    private val fields: MutableMap<String, Any?> = hashMapOf()

    fun get(name: Token): Any? {
        if (name.lexeme == "klass") {
            return klass
        }

        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }

        val method = klass.findMethod(name.lexeme)
        if (method != null) {
            return method.bind(this)
        }

        if (klass is LoxInstance) {
            try {
                return klass.get(name)
            } catch (e: RuntimeError) {
            }
        }

        throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
    }

    override fun toString(): String {
        return klass.name + " instance"
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}
