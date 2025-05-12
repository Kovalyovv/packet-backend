package ru.packet.models

import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.jodatime.timestampWithTimeZone
import org.joda.time.DateTime

object Activities : Table("activities") {
    val id = integer("id").autoIncrement()
    val groupId = reference("group_id", Groups.id, onDelete = ReferenceOption.CASCADE).nullable()
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val type = varchar("type", 20) // "ADDED" или "BOUGHT"
    val itemId = integer("item_id").references(Items.id)
    val quantity = integer("quantity").default(1)
    val price = integer("price").nullable() // Добавляем поле для цены
    val isViewed = bool("is_viewed").default(false)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id, name = "PK_Activities_ID")
}