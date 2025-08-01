package ru.packet.services

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import ru.packet.dto.ProcessedCheckData
import ru.packet.dto.ProcessedCheckItem
import ru.packet.dto.ReceiptDTO
import ru.packet.dto.ReceiptItemDTO
import ru.packet.models.*
import java.sql.SQLIntegrityConstraintViolationException

@Serializable
data class NalogReceiptResponse(
    val code: Int,
    val first: Int? = null,
    val data: NalogReceiptData? = null
)

@Serializable
data class NalogReceiptData(
    val json: NalogReceiptJson? = null,
    val html: String? = null
)

@Serializable
data class NalogReceiptJson(
    val totalSum: Double? = null,
    val user: String? = null,
    val dateTime: String? = null,
    val retailPlace: String? = null,
    val items: List<NalogReceiptItem>? = null
)

@Serializable
data class NalogReceiptItem(
    val name: String,
    val price: Double,
    val quantity: Double,
    val sum: Double? = null,
    val nds: Int? = null,
    val paymentType: Int? = null,
    val productType: Int? = null,
    val productCodeNew: ProductCodeNew? = null,
    val itemsQuantityMeasure: Int? = null
)

@Serializable
data class ProductCodeNew(
    val ean13: Ean13? = null,
    val gs1m: Gs1m? = null
)

@Serializable
data class Ean13(
    val gtin: String? = null,
    val sernum: String? = null,
    val productIdType: Int? = null,
    val rawProductCode: String? = null
)

@Serializable
data class Gs1m(
    val gtin: String? = null,
    val sernum: String? = null,
    val productIdType: Int? = null,
    val rawProductCode: String? = null
)



class ReceiptService(private val database: Database) {

    fun processQrCode(userId: Int, qrCode: String, receiptData: ProcessedCheckData): Pair<ReceiptDTO, ProcessedCheckData> {
        if (qrCode.isBlank()) throw IllegalArgumentException("QR code cannot be blank")

        return transaction(database) {
            val existingReceipt = Receipts.select { Receipts.qrCode eq qrCode }.firstOrNull()
            if (existingReceipt != null) {
                val receiptId = existingReceipt[Receipts.id]
                val receiptItems = ReceiptItems.select { ReceiptItems.receiptId eq receiptId }
                    .map {
                        ProcessedCheckItem(
                            name = it[ReceiptItems.name],
                            price = it[ReceiptItems.price],
                            quantity = it[ReceiptItems.quantity].toDouble()
                        )
                    }

                val receiptDTO = ReceiptDTO(
                    id = receiptId,
                    userId = existingReceipt[Receipts.userId],
                    groupId = existingReceipt[Receipts.groupId],
                    qrCode = existingReceipt[Receipts.qrCode],
                    totalAmount = existingReceipt[Receipts.totalAmount],
                    scannedAt = existingReceipt[Receipts.scannedAt].toString()
                )

                val checkData = ProcessedCheckData(
                    totalSum = existingReceipt[Receipts.totalAmount] ?: 0,
                    items = receiptItems
                )

                Pair(receiptDTO, checkData)
            } else {
                // Чека нет, создаём новый
                println("Inserting receipt for userId=$userId, qrCode=$qrCode")
                val receiptId = Receipts.insert {
                    it[Receipts.userId] = userId
                    it[Receipts.groupId] = 0
                    it[Receipts.qrCode] = qrCode
                    it[Receipts.totalAmount] = receiptData.totalSum
                    it[Receipts.scannedAt] = DateTime.now(DateTimeZone.UTC)
                }[Receipts.id]

                println("Inserted receipt with ID=$receiptId")

                val items = receiptData.items.map { item ->
                    ReceiptItemDTO(
                        receiptId = receiptId,
                        itemId = null,
                        name = item.name,
                        quantity = item.quantity,
                        price = item.price,
                        matched = false
                    )
                }
                println("Saving ${items.size} items for receiptId=$receiptId")
                addItemsToReceiptAndItems(receiptId, items)
                println("Items saved successfully")

                val receiptDTO = ReceiptDTO(
                    id = receiptId,
                    userId = userId,
                    groupId = 0,
                    qrCode = qrCode,
                    totalAmount = receiptData.totalSum,
                    scannedAt = DateTime.now(DateTimeZone.UTC).toString()
                )

                Pair(receiptDTO, receiptData)
            }
        }
    }

    fun addItemsToReceiptAndItems(receiptId: Int, items: List<ReceiptItemDTO>) {
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

    fun confirmReceiptItems(receiptId: Int, userId: Int, groupId: Int?, confirmedItems: List<ProcessedCheckItem>) {
        transaction(database) {
            val receipt = Receipts.select { Receipts.id eq receiptId }.firstOrNull()
                ?: throw IllegalArgumentException("Receipt with ID $receiptId does not exist")

            if (receipt[Receipts.userId] != userId) {
                throw IllegalArgumentException("User ID does not match receipt owner")
            }

            if (groupId != null) {
                Receipts.update({ Receipts.id eq receiptId }) {
                    it[Receipts.groupId] = groupId
                }
            }

            matchReceiptItems(receiptId, groupId == null)
            transferReceiptItemsToHistory(receiptId, groupId == null)
        }
    }

    fun matchReceiptItems(receiptId: Int, isPersonalList: Boolean = false) {
        transaction(database) {
            val receipt = Receipts.select { Receipts.id eq receiptId }.firstOrNull()
                ?: throw IllegalArgumentException("Receipt with ID $receiptId does not exist")

            val groupId = receipt[Receipts.groupId]
            val userId = receipt[Receipts.userId]

            val receiptItems = ReceiptItems.select {
                (ReceiptItems.receiptId eq receiptId) and (ReceiptItems.matched eq false)
            }.toList()

            receiptItems.forEach { receiptItem ->
                val receiptItemName = receiptItem[ReceiptItems.name]

                val matchedItem = if (isPersonalList) {
                    PersonalListItems.join(Items, JoinType.INNER, PersonalListItems.itemId, Items.id)
                        .select { PersonalListItems.userId eq userId }
                        .map { it[Items.name] }
                        .find { validateItemName(receiptItemName, it) }
                } else {
                    GroupListItems.join(Items, JoinType.INNER, GroupListItems.itemId, Items.id)
                        .select { GroupListItems.groupId eq groupId }
                        .map { it[Items.name] }
                        .find { validateItemName(receiptItemName, it) }
                }

                if (matchedItem != null) {
                    val item = Items.select { Items.name eq matchedItem }.firstOrNull()
                    if (item != null) {
                        ReceiptItems.update({ ReceiptItems.id eq receiptItem[ReceiptItems.id] }) {
                            it[ReceiptItems.itemId] = item[Items.id]
                            it[ReceiptItems.matched] = true
                        }
                    }
                }
            }
        }
    }

    fun transferReceiptItemsToHistory(receiptId: Int, isPersonalList: Boolean = false) {
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

                if (isPersonalList) {
                    PersonalListItems.deleteWhere {
                        (PersonalListItems.userId eq userId) and (PersonalListItems.itemId eq itemId)
                    }
                } else {
                    GroupListItems.deleteWhere {
                        (GroupListItems.groupId eq groupId) and (GroupListItems.itemId eq itemId)
                    }
                }

                Activities.insert {
                    it[Activities.groupId] = if (isPersonalList) null else groupId
                    it[Activities.userId] = userId
                    it[Activities.type] = "BOUGHT"
                    it[Activities.itemId] = itemId
                    it[Activities.quantity] = receiptItem[ReceiptItems.quantity].toInt()
                    it[Activities.price] = receiptItem[ReceiptItems.price]
                    it[Activities.isViewed] = false
                }
            }

            val unmatchedItems = ReceiptItems.select {
                ReceiptItems.receiptId eq receiptId and (ReceiptItems.matched eq false)
            }.toList()

            unmatchedItems.forEach { receiptItem ->
                val existingItem = Items.select { Items.name eq receiptItem[ReceiptItems.name] }.firstOrNull()
                val itemId = if (existingItem != null) {
                    existingItem[Items.id]
                } else {
                    Items.insert {
                        it[Items.name] = receiptItem[ReceiptItems.name]
                        it[Items.price] = receiptItem[ReceiptItems.price]
                    } get Items.id
                }

                Activities.insert {
                    it[Activities.groupId] = if (isPersonalList) null else groupId
                    it[Activities.userId] = userId
                    it[Activities.type] = "BOUGHT"
                    it[Activities.itemId] = itemId
                    it[Activities.quantity] = receiptItem[ReceiptItems.quantity].toInt()
                    it[Activities.price] = receiptItem[ReceiptItems.price]
                    it[Activities.isViewed] = false
                }
            }
        }
    }

    fun getReceiptsHistory(userId: Int): List<Pair<ReceiptDTO, ProcessedCheckData>> {
        return transaction(database) {
            Receipts.select { Receipts.userId eq userId }
                .orderBy(Receipts.scannedAt, SortOrder.DESC)
                .map { receipt ->
                    val receiptId = receipt[Receipts.id]
                    val receiptItems = ReceiptItems.select { ReceiptItems.receiptId eq receiptId }
                        .map {
                            ProcessedCheckItem(
                                name = it[ReceiptItems.name],
                                price = it[ReceiptItems.price],
                                quantity = it[ReceiptItems.quantity].toDouble()
                            )
                        }

                    val receiptDTO = ReceiptDTO(
                        id = receipt[Receipts.id],
                        userId = receipt[Receipts.userId],
                        groupId = receipt[Receipts.groupId],
                        qrCode = receipt[Receipts.qrCode],
                        totalAmount = receipt[Receipts.totalAmount],
                        scannedAt = receipt[Receipts.scannedAt].toString()
                    )

                    val checkData = ProcessedCheckData(
                        totalSum = receipt[Receipts.totalAmount] ?: 0,
                        items = receiptItems
                    )

                    Pair(receiptDTO, checkData)
                }
        }
    }

    private fun validateItemName(receiptItemName: String, listItemName: String): Boolean {
        val receiptName = receiptItemName.lowercase().trim()
        val listName = listItemName.lowercase().trim()

        val receiptWords = receiptName.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        val listWords = listName.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()

        val commonWords = receiptWords.intersect(listWords)
        val matchPercentage = (commonWords.size.toDouble() / maxOf(receiptWords.size, listWords.size).toDouble()) * 100

        return matchPercentage >= 70.0
    }

    fun updateReceiptGroup(receiptId: Int, groupId: Int) {
        transaction(database) {
            val receiptExists = Receipts.select { Receipts.id eq receiptId }.firstOrNull()
                ?: throw IllegalArgumentException("Receipt with ID $receiptId does not exist")

            Receipts.update({ Receipts.id eq receiptId }) {
                it[Receipts.groupId] = groupId
            }
        }
    }

    fun markItemAsBought(itemId: Int, buyRequest: BuyItemRequest) {
        transaction(database) {
            val listItem = GroupListItems.select { GroupListItems.id eq itemId }.firstOrNull()
                ?: throw IllegalArgumentException("Item with ID $itemId not found in group list")

            if (listItem[GroupListItems.groupId] != buyRequest.groupId) {
                throw IllegalArgumentException("Item with ID $itemId does not belong to group ${buyRequest.groupId}")
            }

            val remainingQuantity = listItem[GroupListItems.quantity] - buyRequest.quantity
            if (remainingQuantity < 0) {
                throw IllegalArgumentException("Cannot buy more than available quantity (${listItem[GroupListItems.quantity]})")
            }

            if (remainingQuantity == 0) {
                GroupListItems.deleteWhere { GroupListItems.id eq itemId }
            } else {
                GroupListItems.update({ GroupListItems.id eq itemId }) {
                    it[quantity] = remainingQuantity
                }
            }

            Activities.insert {
                it[Activities.groupId] = buyRequest.groupId
                it[Activities.userId] = buyRequest.boughtBy
                it[Activities.itemId] = listItem[GroupListItems.itemId]
                it[Activities.quantity] = buyRequest.quantity
                it[Activities.price] = buyRequest.price
                it[Activities.type] = "BOUGHT"
                it[Activities.isViewed] = false
            }
        }
    }
}