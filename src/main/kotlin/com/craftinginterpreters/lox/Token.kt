package com.craftinginterpreters.lox

data class Token(
    val type: TokenType,
    val lexeme: String,
    val line: Int
)
