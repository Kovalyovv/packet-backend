package ru.packet.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import ru.packet.dto.PersonalListItem
import ru.packet.dto.ProcessedCheckItem
import ru.packet.dto.PurchaseHistoryItem
import ru.packet.models.Items
import ru.packet.models.PersonalListItems
import ru.packet.models.PersonalPurchaseHistory

class PersonalListService {

    fun getPersonalList(userId: Int): List<PersonalListItem> {
        return transaction {
            PersonalListItems.select { PersonalListItems.userId eq userId }
                .map {
                    val addedAtFromDb = it[PersonalListItems.addedAt]
                    PersonalListItem(
                        id = it[PersonalListItems.id],
                        itemId = it[PersonalListItems.itemId],
                        itemName = it[PersonalListItems.itemName],
                        quantity = it[PersonalListItems.quantity],
                        price = it[PersonalListItems.price],
                        addedAt = addedAtFromDb
                            .withZone(DateTimeZone.forID("Europe/Moscow"))
                            .toString()
                    )
                }
        }
    }

    fun getPersonalListItems(userId: Int): List<ProcessedCheckItem> {
        return transaction {
            PersonalListItems.select { PersonalListItems.userId eq userId }
                .map {
                    ProcessedCheckItem(
                        name = it[PersonalListItems.itemName],
                        price = it[PersonalListItems.price],
                        quantity = it[PersonalListItems.quantity].toDouble()
                    )
                }
        }
    }

    fun addItemToPersonalList(
        userId: Int,
        itemId: Int?,
        itemName: String,
        quantity: Int,
        priceItem: Int
    ): PersonalListItem {

        println("Adding item to personal list: userId=$userId, itemId=$itemId, itemName=$itemName, quantity=$quantity, price=$priceItem")
        return transaction {
            // Ищем товар в таблице items по имени
            val existingItem = Items.select { Items.name eq itemName }.firstOrNull()
            println("Existing item: $existingItem")

            // Если товар найден, используем его id, иначе создаём новый
            val actualItemId = existingItem?.get(Items.id) ?: Items.insert {
                it[name] = itemName
                it[price] = price
            }[Items.id]
            println("Actual itemId: $actualItemId")

            // Добавляем товар в personal_list_items с найденным или новым itemId
            val newItem = PersonalListItems.insert {
                it[PersonalListItems.userId] = userId
                it[PersonalListItems.itemId] = actualItemId
                it[PersonalListItems.itemName] = itemName
                it[PersonalListItems.quantity] = quantity
                it[PersonalListItems.price] = priceItem
                it[addedAt] = DateTime.now().withZone(DateTimeZone.UTC)
            }.resultedValues!!.first()
            println("New item inserted: $newItem")

            PersonalListItem(
                id = newItem[PersonalListItems.id],
                itemId = newItem[PersonalListItems.itemId],
                itemName = newItem[PersonalListItems.itemName],
                quantity = newItem[PersonalListItems.quantity],
                price = newItem[PersonalListItems.price],
                addedAt = newItem[PersonalListItems.addedAt]
                    .withZone(DateTimeZone.forID("Europe/Moscow"))
                    .toString()
            )
        }
    }

    fun markAsPurchased(userId: Int, itemId: Int): Boolean {
        return transaction {
            // Находим элемент в личном списке
            val item = PersonalListItems.select {
                (PersonalListItems.id eq itemId) and (PersonalListItems.userId eq userId)
            }.firstOrNull() ?: return@transaction false

            // Добавляем в историю покупок с ценой из personalListItems
            PersonalPurchaseHistory.insert {
                it[PersonalPurchaseHistory.userId] = userId
                it[PersonalPurchaseHistory.itemId] = item[PersonalListItems.itemId]
                it[PersonalPurchaseHistory.itemName] = item[PersonalListItems.itemName]
                it[PersonalPurchaseHistory.quantity] = item[PersonalListItems.quantity]
                it[PersonalPurchaseHistory.price] = item[PersonalListItems.price]
                it[purchasedAt] = DateTime.now().withZone(DateTimeZone.UTC)
            }

            // Удаляем из личного списка
            PersonalListItems.deleteWhere { PersonalListItems.id eq itemId }

            true
        }
    }

    fun getPurchaseHistory(userId: Int): List<PurchaseHistoryItem> {
        return transaction {
            PersonalPurchaseHistory
                .select { PersonalPurchaseHistory.userId eq userId }
                .orderBy(PersonalPurchaseHistory.purchasedAt to SortOrder.DESC)
                .map {
                    PurchaseHistoryItem(
                        id = it[PersonalPurchaseHistory.id],
                        itemId = it[PersonalPurchaseHistory.itemId],
                        itemName = it[PersonalPurchaseHistory.itemName],
                        quantity = it[PersonalPurchaseHistory.quantity],
                        price = it[PersonalPurchaseHistory.price],
                        purchasedAt = it[PersonalPurchaseHistory.purchasedAt]
                            .withZone(DateTimeZone.forID("Europe/Moscow"))
                            .toString()
                    )
                }
        }
    }
}