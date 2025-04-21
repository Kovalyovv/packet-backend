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
            // Вставляем запись в GroupListItems
            val insertedItemId = GroupListItems.insert {
                it[GroupListItems.groupId] = groupId
                it[GroupListItems.itemId] = itemId
                it[GroupListItems.quantity] = quantity
                it[GroupListItems.priority] = priority
                it[GroupListItems.budget] = budget
                it[GroupListItems.isViewed] = false
            }[GroupListItems.id]

            // Извлекаем вставленную запись с JOIN на Items
            val insertedItem = GroupListItems
                .join(Items, JoinType.LEFT, additionalConstraint = { GroupListItems.itemId eq Items.id })
                .select { GroupListItems.id eq insertedItemId }
                .first()

            GroupListItemDTO(
                id = insertedItem[GroupListItems.id],
                groupId = insertedItem[GroupListItems.groupId],
                itemId = insertedItem[GroupListItems.itemId],
                itemName = insertedItem[Items.name] ?: "Unknown", // Извлекаем название товара
                quantity = insertedItem[GroupListItems.quantity],
                priority = insertedItem[GroupListItems.priority],
                budget = insertedItem[GroupListItems.budget],
                isViewed = insertedItem[GroupListItems.isViewed]
            )
        }
    }

    fun getItemsByGroupId(groupId: Int): List<GroupListItemDTO> {
        return transaction(database) {
            GroupListItems
                .join(Items, JoinType.LEFT, additionalConstraint = { GroupListItems.itemId eq Items.id })
                .select { GroupListItems.groupId eq groupId }
                .map {
                    GroupListItemDTO(
                        id = it[GroupListItems.id],
                        groupId = it[GroupListItems.groupId],
                        itemId = it[GroupListItems.itemId],
                        itemName = it[Items.name] ?: "Unknown", // Извлекаем название товара
                        quantity = it[GroupListItems.quantity],
                        priority = it[GroupListItems.priority],
                        budget = it[GroupListItems.budget],
                        isViewed = it[GroupListItems.isViewed]
                    )
                }
        }
    }

    fun markItemAsBought(itemId: Int, buyRequest: BuyItemRequest) {
        transaction(database) {
            val listItem = GroupListItems.select { GroupListItems.id eq itemId }.firstOrNull()
                ?: throw IllegalArgumentException("Item with ID $itemId not found in group list")

            // Проверяем, что groupId из BuyItemRequest совпадает с groupId товара
            val actualGroupId = listItem[GroupListItems.groupId]
            if (actualGroupId != buyRequest.groupId) {
                throw IllegalArgumentException("Item with ID $itemId does not belong to group ${buyRequest.groupId}")
            }

            // Удаляем товар из GroupListItems
            GroupListItems.deleteWhere { GroupListItems.id eq itemId }

            // Создаём запись в Activities с типом "BOUGHT"
            Activities.insert {
                it[Activities.groupId] = actualGroupId
                it[Activities.userId] = buyRequest.boughtBy
                it[Activities.type] = "BOUGHT"
                it[Activities.itemId] = listItem[GroupListItems.itemId]
                it[Activities.quantity] = listItem[GroupListItems.quantity]
                it[Activities.price] = buyRequest.price
                it[Activities.isViewed] = false
            }
        }
    }
}