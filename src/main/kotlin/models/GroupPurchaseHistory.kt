package ru.packet.models


import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object GroupPurchaseHistory : Table("group_purchase_history") {
    val id = integer("id").autoIncrement()
    val groupId = integer("group_id").references(Groups.id)
    val itemId = integer("item_id").references(Items.id)
    val boughtBy = integer("bought_by").references(Users.id)
    val quantity = integer("quantity")
    val price = integer("price").nullable()
    val purchasedAt = timestamp("purchased_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id, name = "PK_GroupPurchaseHistory_ID")
}