package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddItemRequest(
    val itemId: Int? = null,
    val itemName: String,
    val quantity: Int,
    val price: Int
)