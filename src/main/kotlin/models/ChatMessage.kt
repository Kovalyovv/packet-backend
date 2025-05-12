package ru.packet.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Int,
    val groupId: Int,
    val senderId: Int,
    val text: String,
    val timestamp: String,
    val replyToId: Int? = null // Новое поле для ответа на сообщение
)
