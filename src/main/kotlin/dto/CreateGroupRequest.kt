package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupRequest(
    val name: String,
    val creatorId: Int,
    val isPersonal: Boolean? = null // Опциональное поле с дефолтным значением null
)