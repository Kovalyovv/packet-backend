package ru.packet.services

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.packet.dto.ItemDTO
import ru.packet.models.Items

class ItemService(private val database: Database) {
    fun createItem(name: String, barcode: String?, category: String?, price: Int): ItemDTO {
        if (name.isBlank()) throw IllegalArgumentException("Item name cannot be blank")
        if (price < 0) throw IllegalArgumentException("Price cannot be negative")

        return transaction {
            val existingItem = Items.select { Items.name eq name }.firstOrNull()
            if (existingItem != null) throw IllegalArgumentException("Item with name $name already exists")

            Items.insert {
                it[Items.name] = name
                it[Items.barcode] = barcode
                it[Items.category] = category
                it[Items.price] = price
            }

            val insertedItem = Items.select { Items.name eq name }.first()
            ItemDTO(
                id = insertedItem[Items.id],
                name = insertedItem[Items.name],
                barcode = insertedItem[Items.barcode],
                category = insertedItem[Items.category],
                price = insertedItem[Items.price]
            )
        }
    }

    fun getAllItems(): List<ItemDTO> {
        return transaction {
            Items.selectAll().map {
                ItemDTO(
                    id = it[Items.id],
                    name = it[Items.name],
                    barcode = it[Items.barcode],
                    category = it[Items.category],
                    price = it[Items.price]
                )
            }
        }
    }

    fun getItemByBarcode(barcode: String): ItemDTO? {
        if (barcode.isBlank()) throw IllegalArgumentException("Barcode cannot be blank")

        return transaction {
            Items.select { Items.barcode eq barcode }.firstOrNull()?.let {
                ItemDTO(
                    id = it[Items.id],
                    name = it[Items.name],
                    barcode = it[Items.barcode],
                    category = it[Items.category],
                    price = it[Items.price]
                )
            }
        }
    }
}