package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConfirmItemsRequest(
    val receiptId: Int,
    val items: List<ProcessedCheckItem>,
    val groupId: Int? = null // Убедимся, что groupId может быть null
)