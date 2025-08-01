package ru.packet.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import ru.packet.models.*
import ru.packet.dto.ActivityDTO
import ru.packet.dto.BuyItemRequest
import ru.packet.dto.GroupListItemDTO
import ru.packet.dto.GroupSummaryDTO

class ActivityService(private val database: Database) {

    fun addItemToGroupList(groupId: Int, userId: Int, itemId: Int, quantity: Int, priority: Int, budget: Int?): GroupListItemDTO {
        return transaction(database) {
            val item = Items.select { Items.id eq itemId }.single()
            val user = Users.select { Users.id eq userId }.single()

            val insertedItem = GroupListItems.insert {
                it[GroupListItems.groupId] = groupId
                it[GroupListItems.itemId] = itemId
                it[GroupListItems.quantity] = quantity
                it[GroupListItems.priority] = priority
                it[GroupListItems.budget] = budget
            }

            val listItemId = insertedItem[GroupListItems.id]

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
                budget = budget
            )
        }
    }

    fun buyItem(groupListItemId: Int, buyRequest: BuyItemRequest) {
        transaction(database) {
            val listItem = GroupListItems.select { GroupListItems.id eq groupListItemId }.singleOrNull()
                ?: throw IllegalArgumentException("Товар не найден в списке группы")

            if (listItem[GroupListItems.groupId] != buyRequest.groupId) {
                throw IllegalArgumentException("Товар с ID $groupListItemId не принадлежит группе ${buyRequest.groupId}")
            }

            val remainingQuantity = listItem[GroupListItems.quantity] - buyRequest.quantity
            if (remainingQuantity < 0) {
                throw IllegalArgumentException("Нельзя купить больше, чем доступно (${listItem[GroupListItems.quantity]})")
            }

            if (remainingQuantity == 0) {
                // Если всё количество куплено, удаляем запись
                GroupListItems.deleteWhere { GroupListItems.id eq groupListItemId }
            } else {
                // Если куплено частично, обновляем количество
                GroupListItems.update({ GroupListItems.id eq groupListItemId }) {
                    it[quantity] = remainingQuantity
                }
            }

            // Ищем запись в Activities с типом ADDED
            val activity = Activities.select {
                (Activities.groupId eq buyRequest.groupId) and
                        (Activities.itemId eq listItem[GroupListItems.itemId]) and
                        (Activities.type eq "ADDED")
            }.firstOrNull()

            if (activity != null) {
                // Если запись найдена, обновляем её
                Activities.update({ Activities.id eq activity[Activities.id] }) {
                    it[type] = "BOUGHT"
                    it[quantity] = buyRequest.quantity
                    it[price] = buyRequest.price
                    it[userId] = buyRequest.boughtBy
                    it[isViewed] = false
                }
            } else {
                // Если записи нет, создаём новую
                Activities.insert {
                    it[Activities.groupId] = buyRequest.groupId
                    it[Activities.userId] = buyRequest.boughtBy
                    it[Activities.itemId] = listItem[GroupListItems.itemId]
                    it[Activities.quantity] = buyRequest.quantity
                    it[Activities.price] = buyRequest.price
                    it[Activities.type] = "BOUGHT"
                    it[Activities.isViewed] = false
                }
            }
        }
    }

    fun getGroupSummaries(userId: Int, groupIds: List<Int>): List<GroupSummaryDTO> {
        if (groupIds.isEmpty()) return emptyList()

        return transaction(database) {
            val groupsMap = Groups
                .select { Groups.id inList groupIds }
                .associateBy { it[Groups.id] }

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
                    unseenCount = unseenCount
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
                        budget = it[GroupListItems.budget]
                    )
                }
        }
    }

    fun markItemsAsViewed(groupId: Int, itemIds: List<Int>) {
        transaction(database) {
            Activities.update({ (Activities.groupId eq groupId) and (Activities.itemId inList itemIds) }) {
                it[Activities.isViewed] = true
            }
        }
    }

    fun markAllActivitiesAsViewed(groupId: Int) {
        transaction(database) {
            Activities.update({ Activities.groupId eq groupId }) {
                it[Activities.isViewed] = true
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