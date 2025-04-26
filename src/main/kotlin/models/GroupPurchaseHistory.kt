package ru.packet.models


import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.jodatime.timestampWithTimeZone
import ru.packet.models.ChatMessages.defaultExpression

object GroupPurchaseHistory : Table("group_purchase_history") {
    val id = integer("id").autoIncrement()
    val groupId = integer("group_id").references(Groups.id)
    val itemId = integer("item_id").references(Items.id)
    val boughtBy = integer("bought_by").references(Users.id)
    val quantity = integer("quantity")
    val price = integer("price").nullable()
    val purchasedAt = datetime("purchased_at").defaultExpression(timestampWithTimeZone("GMT+3"))

    override val primaryKey = PrimaryKey(id, name = "PK_GroupPurchaseHistory_ID")
}