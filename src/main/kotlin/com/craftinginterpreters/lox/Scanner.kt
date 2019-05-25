package com.craftinginterpreters.lox

class Scanner(private val source: String) {
    private val tokens: ArrayList<Token> = arrayListOf()
    private var start = 0
    private var current = 0
    private var line = 1

    fun scanTokens(): ArrayList<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(TokenType.Eof, "", line))
        return tokens
    }

    private fun isAtEnd(): Boolean = current >= source.length

    private fun scanToken() {

        val tokenType: TokenType = when (val c = advance() ?: return) {
            '(' -> TokenType.LeftParen
            ')' -> TokenType.RightParen
            '{' -> TokenType.LeftBrace
            '}' -> TokenType.RightBrace
            ',' -> TokenType.Comma
            '.' -> TokenType.Dot
            '-' -> TokenType.Minus
            '+' -> TokenType.Plus
            ';' -> TokenType.Semicolon
            '*' -> TokenType.Star
            '!' -> {
                if (doesMatch('=')) {
                    TokenType.BangEqual
                } else {
                    TokenType.Bang
                }
            }
            '=' -> {
                if (doesMatch('=')) {
                    TokenType.EqualEqual
                } else {
                    TokenType.Equal
                }
            }
            '<' -> {
                if (doesMatch('=')) {
                    TokenType.LessEqual
                } else {
                    TokenType.Less
                }
            }
            '>' -> {
                if (doesMatch('=')) {
                    TokenType.GreaterEqual
                } else {
                    TokenType.Greater
                }
            }
            'o' -> {
                if (peek() == 'r') {
                    TokenType.Or
                } else {
                    return
                }
            }
            ' ', '\r', '\t' -> return
            '\n' -> {
                line += 1
                return
            }
            '"' -> {
                string()
                return
            }
            '/' -> {
                if (peek() == '*') {
                    multiLineComment()
                }
                return
            }
            else -> {
                when {
                    isDigit(c) -> number()
                    isAlpha(c) -> identifier()
                    else -> Lox.error(line, "Unexpected character.")
                }
                return
            }
        }
        addToken(tokenType)
    }

    private fun advance(): Char? {
        current += 1
        return source.getOrNull(current - 1)
    }

    private fun peek(): Char? {
        return source.getOrNull(current)
    }

    private fun peekNext(): Char? {
        return source.getOrNull(current + 1)
    }

    private fun addToken(ty: TokenType) {
        val text = source.substring(start, current)
        tokens += Token(ty, text, line)
    }

    private fun doesMatch(@Suppress("SameParameterValue") expected: Char): Boolean {
        if (isAtEnd()) {
            return false
        }

        if (source.getOrNull(current) != expected) {
            return false
        }

        current += 1
        return true
    }

    private fun string() {
        while (peek() != '"') {
            if (peek() != '\n') {
                line += 1
            }
            advance()
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.")
            return
        }

        advance()

        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.String(value))
    }

    private fun isDigit(c: Char): Boolean = c in '0'..'9'

    private fun number() {
        while (peek()?.let { isDigit(it) } == true) {
            advance()
        }

        if (peek() == '.' && peekNext()?.isDigit() == true) {
            advance()
            while (peek()?.isDigit() == true) {
                advance()
            }
        }

        addToken(TokenType.Number(source.substring(start, current).toDouble()))
    }

    private fun identifier() {
        while (peek()?.let { isAlphaNumeric(it) } == true) {
            advance()
        }

        val text = source.substring(start, current)
        val ty = Scanner.keywords.getOrDefault(text, TokenType.Identifier)
        addToken(ty)
    }

    private fun isAlpha(c: Char): Boolean {
        return (c in 'a'..'z') ||
                (c in 'A'..'Z') ||
                c == '_'
    }

    private fun isAlphaNumeric(c: Char): Boolean = isAlpha(c) || isDigit(c)

    private fun multiLineComment() {
        while (!(peek() == '*' && peekNext() == '/')) {
            advance()
        }

        // skip the ending */
        advance()
        advance()
        addToken(TokenType.MultiLineComment(source.substring(start + 2, current - 2)))
    }

    companion object {
        val keywords = hashMapOf(
            "and" to TokenType.And,
            "class" to TokenType.Class,
            "else" to TokenType.Else,
            "false" to TokenType.False,
            "for" to TokenType.For,
            "fun" to TokenType.Fun,
            "if" to TokenType.If,
            "nil" to TokenType.Nil,
            "or" to TokenType.Or,
            "print" to TokenType.Print,
            "return" to TokenType.Return,
            "super" to TokenType.Super,
            "this" to TokenType.This,
            "true" to TokenType.True,
            "var" to TokenType.Var,
            "while" to TokenType.While
        )
    }
}

