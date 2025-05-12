package ru.packet.models


import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.jodatime.timestampWithTimeZone
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import ru.packet.models.Groups.default

object Receipts : Table("receipts") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val groupId = reference("group_id", Groups.id, onDelete = ReferenceOption.CASCADE)
    val qrCode = text("qr_code")
    val totalAmount = integer("total_amount").nullable()
    val scannedAt = datetime("scanned_at").default(DateTime.now(DateTimeZone.UTC))

    override val primaryKey = PrimaryKey(id, name = "PK_Receipts_ID")
}