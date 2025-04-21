package ru.packet.models


import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object ChatMessages : Table("chat_messages") {
    val id = integer("id").autoIncrement()
    val groupId = reference("group_id", Groups.id, onDelete = ReferenceOption.CASCADE)
    val senderId = reference("sender_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val text = text("text")
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id, name = "PK_ChatMessages_ID")
}