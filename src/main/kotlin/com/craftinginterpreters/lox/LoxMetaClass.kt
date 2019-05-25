package com.craftinginterpreters.lox

class LoxMetaClass(override val name: String, val superclass: LoxClass?, val methods: Map<String, LoxFunction>):
    LoxCallable,
    LoxBaseClass {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): LoxBaseClass? {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)
        return instance as? LoxBaseClass
    }

    override fun arity(): Int {
        val initializer = findMethod("init") ?: return 0
        return initializer.arity()
    }

    override fun toString(): String {
        return "$name metaclass"
    }

    override fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods.get(name)
        }

        if (superclass != null) {
            return superclass.findMethod(name)
        }

        return null
    }
}
