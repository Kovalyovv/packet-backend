package ru.packet.models

import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

object PersonalListItems : Table("personal_list_items") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id") references Users.id
    val itemId = integer("item_id") references Items.id
    val itemName = varchar("item_name", 145)
    val quantity = integer("quantity")
    val price = integer("price")
    val addedAt = datetime("added_at")

    override val primaryKey = PrimaryKey(id, name = "PK_PersonalListItems_ID")
}