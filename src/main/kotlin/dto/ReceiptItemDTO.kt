package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptItemDTO(
    val id: Int = 0,
    val receiptId: Int,
    val itemId: Int,
    val name: String,
    val quantity: Int,
    val price: Int,
    val matched: Boolean
)