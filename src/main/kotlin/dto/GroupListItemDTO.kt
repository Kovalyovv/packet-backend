package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroupListItemDTO(
    val id: Int,
    val groupId: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val priority: Int,
    val budget: Int?,

)

