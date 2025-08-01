package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDTO(
    val token: String,
    val groupId: Int,
    val senderId: Int,
    val text: String,
    val timestamp: String,
    val replyToToken: String? = null
)