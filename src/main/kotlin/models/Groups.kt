package ru.packet.models


import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.jodatime.timestampWithTimeZone
import org.joda.time.DateTime


object Groups : Table("groups") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val creatorId = reference("creator_id", Users.id).nullable()
    val isPersonal = bool("is_personal").default(false)
    val inviteCode = varchar("invite_code", 10).uniqueIndex()

    val createdAt = datetime("created_at").default(DateTime.now())
//    val inviteCodeIndex = index(isUnique = true, columns = arrayOf(inviteCode))
    override val primaryKey = PrimaryKey(id, name = "PK_Groups_ID")
}
