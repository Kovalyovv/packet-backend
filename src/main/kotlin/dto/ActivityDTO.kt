package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class ActivityDTO(
    val id: Int,
    val groupId: Int,
    val userId: Int,
    val userName: String,
    val type: String,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val isViewed: Boolean,
    val createdAt: String
)