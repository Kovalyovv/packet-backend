package ru.packet.services

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import ru.packet.dto.ActivityDTO
import ru.packet.dto.GroupDTO
import ru.packet.dto.GroupSummaryDTO
import ru.packet.dto.UserDTO
import ru.packet.models.*

class GroupService(private val database: Database) {

    fun createGroup(name: String, creatorId: Int, isPersonal: Boolean? = null): GroupDTO {
        return transaction(database) {
            val code = generateInviteCode()
            val insertedGroup = Groups.insert {
                it[Groups.name] = name
                it[Groups.creatorId] = creatorId
                it[Groups.inviteCode] = code
                if (isPersonal != null) {
                    it[Groups.isPersonal] = isPersonal
                }
            }

            val groupId = insertedGroup[Groups.id]
            val insertedRow = Groups.select { Groups.id eq groupId }.first()

            GroupMembers.insert {
                it[GroupMembers.group] = groupId
                it[GroupMembers.user] = creatorId
            }

            GroupDTO(
                id = groupId,
                name = name,
                members = listOf(creatorId),
                isPersonal = insertedRow[Groups.isPersonal],
                createdAt = insertedRow[Groups.createdAt].toString(),
                inviteCode = code
            )
        }
    }

    fun addUserToGroup(groupId: Int, userId: Int) {
        transaction(database) {
            GroupMembers.insert {
                it[GroupMembers.group] = groupId
                it[GroupMembers.user] = userId
            }
        }
    }

    fun getUserGroups(userId: Int): List<GroupDTO> {
        return transaction(database) {
            val groups = Groups
                .join(GroupMembers, JoinType.INNER, additionalConstraint = { Groups.id eq GroupMembers.group })
                .select { GroupMembers.user eq userId }
                .map { row ->
                    row[Groups.id] to GroupDTO(
                        id = row[Groups.id],
                        name = row[Groups.name],
                        members = emptyList(),
                        isPersonal = row[Groups.isPersonal],
                        createdAt = row[Groups.createdAt].toString(),
                        inviteCode = row[Groups.inviteCode]
                    )
                }
                .toMap()

            val groupIds = groups.keys
            val membersByGroup = GroupMembers
                .select { GroupMembers.group inList groupIds }
                .groupBy { it[GroupMembers.group] }
                .mapValues { entry -> entry.value.map { it[GroupMembers.user] } }

            groups.map { (groupId, groupDTO) ->
                groupDTO.copy(members = membersByGroup[groupId] ?: emptyList())
            }
        }
    }

    fun joinGroup(userId: Int, inviteCode: String): JoinGroupResponse {
        return transaction(database) {
            val group = Groups.select { Groups.inviteCode eq inviteCode }.firstOrNull()
                ?: throw IllegalArgumentException("Group with invite code $inviteCode not found")

            val groupId = group[Groups.id]

            val alreadyMember = GroupMembers.select {
                (GroupMembers.group eq groupId) and (GroupMembers.user eq userId)
            }.count() > 0

            if (alreadyMember) {
                throw IllegalStateException("User $userId is already a member of group $groupId")
            }

            addUserToGroup(groupId, userId)
            val members = GroupMembers
                .select { GroupMembers.group eq groupId }
                .map { it[GroupMembers.user] }

            JoinGroupResponse(
                groupId = groupId,
                groupName = group[Groups.name],
                message = "Пользователь $userId успешно вошел в группу №$groupId"
            )
        }
    }

    fun getGroupSummaries(userId: Int, groupIds: List<Int>): List<GroupSummaryDTO> {
        if (groupIds.isEmpty()) return emptyList()

        return transaction(database) {

            val groupsMap = Groups
                .select { Groups.id inList groupIds }
                .associateBy { it[Groups.id] }

            // Получаем последнюю активность для каждой группы
            val lastActivities = Activities
                .join(Users, JoinType.LEFT, additionalConstraint = { Activities.userId eq Users.id })
                .join(Items, JoinType.LEFT, additionalConstraint = { Activities.itemId eq Items.id })
                .select { Activities.groupId inList groupIds }
                .orderBy(Activities.createdAt to SortOrder.DESC)
                .groupBy { it[Activities.groupId] }
                .mapValues { entry ->
                    entry.value.firstOrNull()?.let {
                        ActivityDTO(
                            id = it[Activities.id],
                            groupId = it[Activities.groupId],
                            userId = it[Activities.userId],
                            userName = it[Users.name] ?: "Unknown",
                            type = it[Activities.type],
                            itemId = it[Activities.itemId],
                            itemName = it[Items.name],
                            quantity = it[Activities.quantity],
                            isViewed = it[Activities.isViewed],
                            createdAt = it[Activities.createdAt].toString()
                        )
                    }
                }

            // Получаем количество непросмотренных активностей
            val unseenCounts = Activities
                .select { (Activities.groupId inList groupIds) and (Activities.isViewed eq false) }
                .groupBy { it[Activities.groupId] }
                .mapValues { entry -> entry.value.count().toInt() }

            groupIds.mapNotNull { groupId ->
                val group = groupsMap[groupId] ?: return@mapNotNull null
                val lastActivity = lastActivities[groupId]
                val unseenCount = unseenCounts[groupId] ?: 0

                GroupSummaryDTO(
                    groupId = groupId,
                    groupName = group[Groups.name],
                    lastActivity = lastActivity,
                    unseenCount = if (lastActivity != null && !lastActivity.isViewed) {
                        unseenCount - 1
                    } else {
                        unseenCount
                    }
                )
            }
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..10).map { chars.random() }.joinToString("")
    }

    fun getInviteCode(groupId: Int): String? {
        return transaction {
            val group = Groups.select { Groups.id eq groupId }.singleOrNull()
            if (group == null) {
                println("Failed invite code get")
                return@transaction null
            }

            group[Groups.inviteCode]
        }
    }
}

@Serializable
data class JoinGroupResponse(
    val groupId: Int,
    val groupName: String,
    val message: String
)