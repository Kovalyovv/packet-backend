package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class BuyItemRequest(
    val groupId: Int, // Добавляем groupId
    val boughtBy: Int,
    val price: Int,
    val quantity: Int
)