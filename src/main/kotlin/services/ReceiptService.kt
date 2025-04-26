package ru.packet.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import ru.packet.dto.ReceiptDTO
import ru.packet.dto.ReceiptItemDTO
import ru.packet.models.*

class ReceiptService(private val database: Database) {
    fun saveReceipt(userId: Int, groupId: Int, qrCode: String, totalAmount: Int?, scannedAt: String): ReceiptDTO {
        if (qrCode.isBlank()) throw IllegalArgumentException("QR code cannot be blank")

        return transaction(database) {
            val receiptId = Receipts.insert {
                it[Receipts.userId] = userId
                it[Receipts.groupId] = groupId
                it[Receipts.qrCode] = qrCode
                it[Receipts.totalAmount] = totalAmount
                it[Receipts.scannedAt] = DateTime.parse(scannedAt)
            }[Receipts.id]

            ReceiptDTO(
                id = receiptId,
                userId = userId,
                groupId = groupId,
                qrCode = qrCode,
                totalAmount = totalAmount,
                scannedAt = scannedAt
            )
        }
    }

    fun addItemsToReceipt(receiptId: Int, items: List<ReceiptItemDTO>) {
        if (items.isEmpty()) throw IllegalArgumentException("Items list cannot be empty")

        transaction(database) {
            val receiptExists = Receipts.select { Receipts.id eq receiptId }.firstOrNull()
                ?: throw IllegalArgumentException("Receipt with ID $receiptId does not exist")

            items.forEach { item ->
                if (item.receiptId != receiptId) throw IllegalArgumentException("Receipt ID mismatch in item")
                if (item.name.isBlank()) throw IllegalArgumentException("Item name cannot be blank")
                if (item.quantity <= 0) throw IllegalArgumentException("Quantity must be positive")
                if (item.price < 0) throw IllegalArgumentException("Price cannot be negative")

                ReceiptItems.insert {
                    it[ReceiptItems.receiptId] = receiptId
                    it[ReceiptItems.itemId] = item.itemId
                    it[ReceiptItems.name] = item.name
                    it[ReceiptItems.quantity] = item.quantity
                    it[ReceiptItems.price] = item.price
                    it[ReceiptItems.matched] = item.matched
                }
            }
        }
    }

    fun matchReceiptItems(receiptId: Int) {
        transaction(database) {
            val receiptExists = Receipts.select { Receipts.id eq receiptId }.firstOrNull()
                ?: throw IllegalArgumentException("Receipt with ID $receiptId does not exist")

            val receiptItems = ReceiptItems.select { ReceiptItems.receiptId eq receiptId }.toList()
            receiptItems.forEach { receiptItem ->
                val item = Items.select { Items.name eq receiptItem[ReceiptItems.name] }.firstOrNull()
                if (item != null) {
                    ReceiptItems.update({ ReceiptItems.id eq receiptItem[ReceiptItems.id] }) {
                        it[ReceiptItems.itemId] = item[Items.id]
                        it[ReceiptItems.matched] = true
                    }
                }
            }
        }
    }

    fun transferReceiptItemsToHistory(receiptId: Int) {
        transaction(database) {
            val receipt = Receipts.select { Receipts.id eq receiptId }.firstOrNull()
                ?: throw IllegalArgumentException("Receipt with ID $receiptId does not exist")

            val groupId = receipt[Receipts.groupId]
            val userId = receipt[Receipts.userId]
            val receiptItems = ReceiptItems.select {
                ReceiptItems.receiptId eq receiptId and (ReceiptItems.matched eq true)
            }.toList()

            receiptItems.forEach { receiptItem ->
                val itemId = receiptItem[ReceiptItems.itemId]
                    ?: throw IllegalArgumentException("Item ID is null for matched receipt item")

                val listItem = GroupListItems.select {
                    (GroupListItems.groupId eq groupId) and
                            (GroupListItems.itemId eq itemId)
                }.firstOrNull()

                if (listItem != null) {
                    // Удаляем товар из списка покупок
                    GroupListItems.deleteWhere { GroupListItems.id eq listItem[GroupListItems.id] }

                    // Создаём запись в Activities с типом "BOUGHT"
                    Activities.insert {
                        it[Activities.groupId] = groupId
                        it[Activities.userId] = userId
                        it[Activities.type] = "BOUGHT"
                        it[Activities.itemId] = itemId
                        it[Activities.quantity] = receiptItem[ReceiptItems.quantity]
                        it[Activities.price] = receiptItem[ReceiptItems.price]
                        it[Activities.isViewed] = false
                    }
                }
            }
        }
    }
}