package com.craftinginterpreters.lox

open class LoxClass(
    override val name: String,
    val superclass: LoxClass?,
    val methods: Map<String, LoxFunction>,
    val metaClass: LoxMetaClass = LoxMetaClass(name, null, emptyMap())
): LoxInstance(metaClass), LoxCallable, LoxBaseClass {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): LoxInstance? {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)
        return instance
    }

    override fun arity(): Int {
        val initializer = findMethod("init") ?: return 0
        return initializer.arity()
    }

    override fun toString(): String {
        return name
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
