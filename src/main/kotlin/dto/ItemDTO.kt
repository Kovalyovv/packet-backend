package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class ItemDTO(
    val id: Int,
    val name: String,
    val barcode: String?,
    val category: String?,
    val price: Int
)