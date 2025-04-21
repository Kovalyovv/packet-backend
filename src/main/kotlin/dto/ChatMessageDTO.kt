package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDTO(
    val id: Int = 0,
    val groupId: Int,
    val senderId: Int,
    val text: String,
    val timestamp: String
)