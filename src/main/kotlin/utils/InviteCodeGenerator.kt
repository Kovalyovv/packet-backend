package ru.packet.utils

import java.security.SecureRandom

object InviteCodeGenerator {
    private val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val random = SecureRandom()

    fun generateCode(length: Int = 8): String {
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}