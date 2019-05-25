package com.craftinginterpreters.lox

sealed class TokenType {
    // Single-character tokens.
    object LeftParen: TokenType()

    object RightParen: TokenType()

    object LeftBrace: TokenType()

    object RightBrace: TokenType()

    object Comma: TokenType()

    object Dot: TokenType()

    object Minus: TokenType()

    object Plus: TokenType()

    object Semicolon: TokenType()

    object Slash: TokenType()

    object Star: TokenType()

    // One or two character tokens.
    object Bang: TokenType()

    object BangEqual: TokenType()

    object Equal: TokenType()

    object EqualEqual: TokenType()

    object Greater: TokenType()

    object GreaterEqual: TokenType()

    object Less: TokenType()

    object LessEqual: TokenType()

    // Literals.
    object Identifier: TokenType()

    data class String(val value: kotlin.String): TokenType()

    data class Number(val value: Double): TokenType()

    // Keywords.
    object And: TokenType()

    object Class: TokenType()

    object Else: TokenType()

    object False: TokenType()

    object Fun: TokenType()

    object For: TokenType()

    object If: TokenType()

    object Nil: TokenType()

    object Or: TokenType()

    object Print: TokenType()

    object Return: TokenType()

    object Super: TokenType()

    object This: TokenType()

    object True: TokenType()

    object Var: TokenType()

    object While: TokenType()

    object Eof: TokenType()

    class MultiLineComment(val value: kotlin.String): TokenType()
}
