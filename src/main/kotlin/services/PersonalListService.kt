package ru.packet.services

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import ru.packet.dto.PersonalListItem
import ru.packet.models.PersonalListItems
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import ru.packet.dto.PurchaseHistoryItem
import ru.packet.models.PersonalPurchaseHistory


class PersonalListService {
    fun getPersonalList(userId: Int): List<PersonalListItem> {
        return transaction {
            PersonalListItems.select { PersonalListItems.userId eq userId }
                .map {
                    val addedAtFromDb = it[PersonalListItems.addedAt] // Это org.joda.time.DateTime
                    PersonalListItem(
                        id = it[PersonalListItems.id],
                        itemId = it[PersonalListItems.itemId],
                        itemName = it[PersonalListItems.itemName],
                        quantity = it[PersonalListItems.quantity],
                        addedAt = addedAtFromDb
                            .withZone(DateTimeZone.forID("Europe/Moscow"))
                            .toString()
                    )
                }
        }
    }

    fun addItemToPersonalList(userId: Int, itemId: Int, itemName: String, quantity: Int): PersonalListItem {
        return transaction {
            val newItem = PersonalListItems.insert {
                it[PersonalListItems.userId] = userId
                it[PersonalListItems.itemId] = itemId
                it[PersonalListItems.itemName] = itemName
                it[PersonalListItems.quantity] = quantity
                it[addedAt] = DateTime.now().withZone(DateTimeZone.UTC) // Сохраняем в UTC
            }.resultedValues!!.first()

            PersonalListItem(
                id = newItem[PersonalListItems.id],
                itemId = newItem[PersonalListItems.itemId],
                itemName = newItem[PersonalListItems.itemName],
                quantity = newItem[PersonalListItems.quantity],
                addedAt = newItem[PersonalListItems.addedAt]
                    .withZone(DateTimeZone.forID("Europe/Moscow"))
                    .toString()
            )
        }
    }

    fun markAsPurchased(userId: Int, itemId: Int, price: Int): Boolean {
        return transaction {
            // Находим элемент в личном списке
            val item = PersonalListItems.select {
                (PersonalListItems.id eq itemId) and (PersonalListItems.userId eq userId)
            }.firstOrNull() ?: return@transaction false

            // Добавляем в историю покупок
            PersonalPurchaseHistory.insert {
                it[PersonalPurchaseHistory.userId] = userId
                it[PersonalPurchaseHistory.itemId] = item[PersonalListItems.itemId]
                it[PersonalPurchaseHistory.itemName] = item[PersonalListItems.itemName]
                it[PersonalPurchaseHistory.quantity] = item[PersonalListItems.quantity]
                it[PersonalPurchaseHistory.price] = price
                it[purchasedAt] = DateTime.now().withZone(DateTimeZone.UTC)
            }

            // Удаляем из личного списка
            PersonalListItems.deleteWhere { PersonalListItems.id eq itemId }

            true
        }
    }

    fun getPurchaseHistory(userId: Int): List<PurchaseHistoryItem> {
        return transaction {
            PersonalPurchaseHistory.select { PersonalPurchaseHistory.userId eq userId }
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

