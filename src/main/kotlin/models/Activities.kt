package ru.packet.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.ReferenceOption

object Activities : Table("activities") {
    val id = integer("id").autoIncrement()
    val groupId = reference("group_id", Groups.id, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val type = varchar("type", 20) // "ADDED" или "BOUGHT"
    val itemId = integer("item_id").references(Items.id)
    val quantity = integer("quantity").default(1)
    val price = integer("price").nullable() // Добавляем поле для цены
    val isViewed = bool("is_viewed").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    override val primaryKey = PrimaryKey(id, name = "PK_Activities_ID")
}