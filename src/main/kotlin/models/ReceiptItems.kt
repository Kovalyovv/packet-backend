package ru.packet.models

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.Table

object ReceiptItems : Table("receipt_items") {
    val id = integer("id").autoIncrement()
    val receiptId = integer("receipt_id").references(Receipts.id, onDelete = ReferenceOption.CASCADE)
    val itemId = integer("item_id").references(Items.id)
    val name = varchar("name", 50)
    val quantity = integer("quantity")
    val price = integer("price")
    val matched = bool("matched").default(false)

    override val primaryKey = PrimaryKey(id, name = "PK_ReceiptItems_ID")
}