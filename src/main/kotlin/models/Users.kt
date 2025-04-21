package ru.packet.models



import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp

object Users: Table("users" ) {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 40)
    val email = varchar("email", 40).uniqueIndex()
    val passwordHash = text("password_hash")
    val role = text("role")

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id, name = "PK_Users_ID")

}