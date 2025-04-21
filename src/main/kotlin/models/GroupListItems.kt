package ru.packet.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object GroupListItems : Table("group_list_items") {
    val id = integer("id").autoIncrement()
    val groupId = reference("group_id", Groups.id, onDelete = ReferenceOption.CASCADE)
    val itemId = integer("item_id").references(Items.id)
    val quantity = integer("quantity").default(1)
    val priority = integer("priority").default(0)
    val budget = integer("budget").nullable()
    val isViewed = bool("is_viewed").default(false) // Добавляем флаг просмотра
    override val primaryKey = PrimaryKey(id, name = "PK_GroupListItems_ID")
}