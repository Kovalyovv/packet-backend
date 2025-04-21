package ru.packet.models


import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object GroupMembers : Table("group_members") {
    val id = integer("id").autoIncrement()
    val user = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val group = reference("group_id", Groups.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(user, group)
}
