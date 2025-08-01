package ru.packet.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import ru.packet.dto.BuyItemRequest
import ru.packet.dto.GroupListItemDTO
import ru.packet.models.*

class ListService(private val database: Database) {
    fun addItemToGroupList(
        groupId: Int,
        itemId: Int,
        quantity: Int,
        priority: Int,
        budget: Int?
    ): GroupListItemDTO {
        return transaction(database) {
            val insertedItemId = GroupListItems.insert {
                it[GroupListItems.groupId] = groupId
                it[GroupListItems.itemId] = itemId
                it[GroupListItems.quantity] = quantity
                it[GroupListItems.priority] = priority
                it[GroupListItems.budget] = budget
            }[GroupListItems.id]

            val insertedItem = GroupListItems
                .join(Items, JoinType.LEFT, additionalConstraint = { GroupListItems.itemId eq Items.id })
                .select { GroupListItems.id eq insertedItemId }
                .first()

            GroupListItemDTO(
                id = insertedItem[GroupListItems.id],
                groupId = insertedItem[GroupListItems.groupId],
                itemId = insertedItem[GroupListItems.itemId],
                itemName = insertedItem[Items.name] ?: "Unknown",
                quantity = insertedItem[GroupListItems.quantity],
                priority = insertedItem[GroupListItems.priority],
                budget = insertedItem[GroupListItems.budget]
            )
        }
    }

//    fun getItemsByGroupId(groupId: Int): List<GroupListItemDTO> {
//        return transaction(database) {
//            GroupListItems
//                .join(Items, JoinType.LEFT, additionalConstraint = { GroupListItems.itemId eq Items.id })
//                .select { GroupListItems.groupId eq groupId }
//                .map {
//                    GroupListItemDTO(
//                        id = it[GroupListItems.id],
//                        groupId = it[GroupListItems.groupId],
//                        itemId = it[GroupListItems.itemId],
//                        itemName = it[Items.name] ?: "Unknown", // Извлекаем название товара
//                        quantity = it[GroupListItems.quantity],
//                        priority = it[GroupListItems.priority],
//                        budget = it[GroupListItems.budget],
//                        isViewed = it[GroupListItems.isViewed]
//                    )
//                }
//        }
//    }

//    fun buyItem(groupListItemId: Int, buyRequest: BuyItemRequest) {
//        transaction(database) {
//            val listItem = GroupListItems.select { GroupListItems.id eq groupListItemId }.singleOrNull()
//                ?: throw IllegalArgumentException("Товар не найден в списке группы")
//
//            if (listItem[GroupListItems.groupId] != buyRequest.groupId) {
//                throw IllegalArgumentException("Товар с ID $groupListItemId не принадлежит группе ${buyRequest.groupId}")
//            }
//
//            val remainingQuantity = listItem[GroupListItems.quantity] - buyRequest.quantity
//            if (remainingQuantity < 0) {
//                throw IllegalArgumentException("Нельзя купить больше, чем доступно (${listItem[GroupListItems.quantity]})")
//            }
//
//            if (remainingQuantity == 0) {
//                // Если всё количество куплено, удаляем запись
//                GroupListItems.deleteWhere { GroupListItems.id eq groupListItemId }
//            } else {
//                // Если куплено частично, обновляем количество
//                GroupListItems.update({ GroupListItems.id eq groupListItemId }) {
//                    it[quantity] = remainingQuantity
//                }
//            }
//
//            // Ищем запись в Activities с типом ADDED
//            val activity = Activities.select {
//                (Activities.groupId eq buyRequest.groupId) and
//                        (Activities.itemId eq listItem[GroupListItems.itemId]) and
//                        (Activities.type eq "ADDED")
//            }.firstOrNull()
//
//            if (activity != null) {
//                // Если запись найдена, обновляем её
//                Activities.update({ Activities.id eq activity[Activities.id] }) {
//                    it[type] = "BOUGHT"
//                    it[quantity] = buyRequest.quantity
//                    it[price] = buyRequest.price
//                    it[userId] = buyRequest.boughtBy
//                    it[isViewed] = false
//                }
//            } else {
//                // Если записи нет, создаём новую
//                Activities.insert {
//                    it[Activities.groupId] = buyRequest.groupId
//                    it[Activities.userId] = buyRequest.boughtBy
//                    it[Activities.itemId] = listItem[GroupListItems.itemId]
//                    it[Activities.quantity] = buyRequest.quantity
//                    it[Activities.price] = buyRequest.price
//                    it[Activities.type] = "BOUGHT"
//                    it[Activities.isViewed] = false
//                }
//            }
//        }
//    }

    fun markItemAsBought(itemId: Int, buyRequest: BuyItemRequest) {
        transaction(database) {
            val listItem = GroupListItems.select { GroupListItems.id eq itemId }.firstOrNull()
                ?: throw IllegalArgumentException("Item with ID $itemId not found in group list")

            if (listItem[GroupListItems.groupId] != buyRequest.groupId) {
                throw IllegalArgumentException("Item with ID $itemId does not belong to group ${buyRequest.groupId}")
            }

            val remainingQuantity = listItem[GroupListItems.quantity] - buyRequest.quantity
            if (remainingQuantity < 0) {
                throw IllegalArgumentException("Cannot buy more than available quantity (${listItem[GroupListItems.quantity]})")
            }

            if (remainingQuantity == 0) {
                GroupListItems.deleteWhere { GroupListItems.id eq itemId }
            } else {
                GroupListItems.update({ GroupListItems.id eq itemId }) {
                    it[quantity] = remainingQuantity
                }
            }

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