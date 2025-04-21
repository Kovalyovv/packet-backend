package ru.packet.models


import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object Receipts : Table("receipts") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val groupId = reference("group_id", Groups.id, onDelete = ReferenceOption.CASCADE)
    val qrCode = text("qr_code")
    val totalAmount = integer("total_amount").nullable()
    val scannedAt = timestamp("scanned_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id, name = "PK_Receipts_ID")
}