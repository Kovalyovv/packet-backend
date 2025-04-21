package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroupDTO(
    val id: Int,
    val name: String,
    val members: List<Int>,
    val isPersonal: Boolean,
    val createdAt: String
)

@Serializable
data class GroupSummaryDTO(
    val groupId: Int,
    val groupName: String,
    val lastActivity: ActivityDTO?,
    val unseenCount: Int
)

