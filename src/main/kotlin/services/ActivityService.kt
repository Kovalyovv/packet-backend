package ru.packet.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import ru.packet.models.*
import ru.packet.dto.ActivityDTO
import ru.packet.dto.GroupListItemDTO
import ru.packet.dto.GroupSummaryDTO

class ActivityService(private val database: Database) {

    fun addItemToGroupList(groupId: Int, userId: Int, itemId: Int, quantity: Int, priority: Int, budget: Int?): GroupListItemDTO {
        return transaction(database) {
            val item = Items.select { Items.id eq itemId }.single()

            val user = Users.select { Users.id eq userId }.single()

            // Добавляем запись в GroupListItems
            val insertedItem = GroupListItems.insert {
                it[GroupListItems.groupId] = groupId
                it[GroupListItems.itemId] = itemId
                it[GroupListItems.quantity] = quantity
                it[GroupListItems.priority] = priority
                it[GroupListItems.budget] = budget
                it[GroupListItems.isViewed] = false
            }

            // Извлекаем id из результата вставки
            val listItemId = insertedItem[GroupListItems.id]

            // Создаём активность ADDED
            Activities.insert {
                it[Activities.groupId] = groupId
                it[Activities.userId] = userId
                it[Activities.type] = "ADDED"
                it[Activities.itemId] = itemId
                it[Activities.quantity] = quantity
                it[Activities.isViewed] = false
            }

            GroupListItemDTO(
                id = listItemId,
                groupId = groupId,
                itemId = itemId,
                itemName = item[Items.name] ?: "Unknown",
                quantity = quantity,
                priority = priority,
                budget = budget,
                isViewed = false
            )
        }
    }

    fun buyItem(groupListItemId: Int, userId: Int, groupId: Int) {
        transaction(database) {
            val listItem = GroupListItems.select { GroupListItems.id eq groupListItemId }.singleOrNull()
            if (listItem == null) {
                throw Exception("Товар не найден в списке группы")
            }

            val itemId = listItem[GroupListItems.itemId]
            val quantity = listItem[GroupListItems.quantity]

            // Удаляем из GroupListItems
            GroupListItems.deleteWhere { GroupListItems.id eq groupListItemId }

            // Создаём активность BOUGHT
            Activities.insert {
                it[Activities.groupId] = groupId
                it[Activities.userId] = userId
                it[Activities.type] = "BOUGHT"
                it[Activities.itemId] = itemId
                it[Activities.quantity] = quantity
                it[Activities.isViewed] = false
            }
        }
    }

    fun getGroupSummaries(userId: Int, groupIds: List<Int>): List<GroupSummaryDTO> {
        if (groupIds.isEmpty()) return emptyList()

        return transaction(database) {
            // Получаем все группы одним запросом
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

    fun getGroupItems(groupId: Int): List<GroupListItemDTO> {
        return transaction(database) {
            GroupListItems
                .join(Items, JoinType.LEFT, additionalConstraint = { GroupListItems.itemId eq Items.id })
                .select { GroupListItems.groupId eq groupId }
                .orderBy(GroupListItems.id to SortOrder.DESC)
                .map {
                    GroupListItemDTO(
                        id = it[GroupListItems.id],
                        groupId = it[GroupListItems.groupId],
                        itemId = it[GroupListItems.itemId],
                        itemName = it[Items.name] ?: "Unknown",
                        quantity = it[GroupListItems.quantity],
                        priority = it[GroupListItems.priority],
                        budget = it[GroupListItems.budget],
                        isViewed = it[GroupListItems.isViewed]
                    )
                }
        }
    }

    fun markItemsAsViewed(groupId: Int, itemIds: List<Int>) {
        transaction(database) {
            GroupListItems.update({ (GroupListItems.groupId eq groupId) and (GroupListItems.id inList itemIds) }) {
                it[GroupListItems.isViewed] = true
            }
        }
    }

    fun markActivitiesAsViewed(groupId: Int, activityIds: List<Int>) {
        transaction(database) {
            Activities.update({ (Activities.groupId eq groupId) and (Activities.id inList activityIds) }) {
                it[Activities.isViewed] = true
            }
        }
    }
}