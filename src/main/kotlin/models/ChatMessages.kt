package ru.packet.models


import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.jodatime.timestampWithTimeZone
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import ru.packet.models.Activities.defaultExpression

object ChatMessages : Table("chat_messages") {
    val token = varchar("token", 36)
    val groupId = reference("group_id", Groups.id, onDelete = ReferenceOption.CASCADE)
    val senderId = reference("sender_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val text = text("text")
    val timestamp = datetime("timestamp").default(DateTime.now(DateTimeZone.UTC))
    val replyToToken = varchar("reply_to_token", 36).nullable()
    override val primaryKey = PrimaryKey(token, name = "PK_ChatMessages_ID")
}