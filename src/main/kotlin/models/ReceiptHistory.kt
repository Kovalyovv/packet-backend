package ru.packet.models

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table



//object ReceiptHistory : Table("receipt_history") {
//    val id = integer("id").autoIncrement()
//    val receiptId = integer("receipt_id").references(Receipts.id, onDelete = ReferenceOption.CASCADE)
//    val name = varchar("name", 145)
//    val quantity = double("quantity")
//    val price = integer("price")
//    val applied = bool("applied").default(false)
//
//    override val primaryKey = PrimaryKey(id, name = "PK_ReceiptHistory_ID")
//}