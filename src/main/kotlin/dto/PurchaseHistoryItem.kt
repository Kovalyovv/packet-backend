package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class PurchaseHistoryItem(
    val id: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val price: Int,
    val purchasedAt: String
)