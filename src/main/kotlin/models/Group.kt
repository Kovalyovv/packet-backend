package ru.packet.models

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int,
    val name: String,
    val creatorId: Int,
    val isPersonal: Boolean,
    val inviteCode: String?,
    val createdAt: String
)