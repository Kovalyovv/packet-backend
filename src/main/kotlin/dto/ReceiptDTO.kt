package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptDTO(
    val id: Int = 0,
    val userId: Int,
    val groupId: Int?,
    val qrCode: String,
    val totalAmount: Int?,
    val scannedAt: String
)

@Serializable
data class ProcessedCheckItem(
    val name: String,
    val price: Int, // Цена в Int с округлением вверх
    val quantity: Double,


)

@Serializable
data class ProcessedCheckData(
    val totalSum: Int, // Общая сумма в Int с округлением вверх
    val items: List<ProcessedCheckItem> // Список товаров с округлёнными ценами
)