package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptHistoryRequest(
    val receiptId: Int,
    val items: List<ProcessedCheckItem>
)