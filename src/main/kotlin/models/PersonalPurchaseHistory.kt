package ru.packet.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime

object PersonalPurchaseHistory : Table("personal_purchase_history") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id") references Users.id
    val itemId = integer("item_id") references Items.id
    val itemName = varchar("item_name", 60)
    val quantity = integer("quantity")
    val price = integer("price") // Цена товара на момент покупки
    val purchasedAt = datetime("purchased_at") // Время покупки

    override val primaryKey = PrimaryKey(id, name = "PK_PersonalPurchaseHistory_ID")
}