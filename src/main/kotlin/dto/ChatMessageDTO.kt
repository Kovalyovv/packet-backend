package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDTO(
    val id: Int,
    val groupId: Int,
    val senderId: Int,
    val text: String,
    val timestamp: String,
    val replyToId: Int? = null
)