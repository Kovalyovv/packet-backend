package ru.packet.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.*

object User: Table("users") {

    val id = integer("id").autoIncrement()
    val name = varchar("name", 40)
    val email = varchar("email", 40).uniqueIndex()
    val passwordHash = text("password_hash")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}