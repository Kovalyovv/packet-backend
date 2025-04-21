package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptDTO(
    val id: Int = 0,
    val userId: Int,
    val groupId: Int,
    val qrCode: String,
    val totalAmount: Int?,
    val scannedAt: String
)