package com.craftinginterpreters.lox

interface LoxBaseClass {
    val name: String

    fun findMethod(name: String): LoxFunction?
}
