package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokenRefreshRequest(
    val refreshToken: String
)