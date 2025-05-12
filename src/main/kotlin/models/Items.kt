package ru.packet.models


import org.jetbrains.exposed.sql.Table


object Items : Table("items") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 145)
    val barcode = varchar("barcode", 50).nullable()
    val category = varchar("category", 60).nullable()
    val price = integer("price").default(0)

    override val primaryKey = PrimaryKey(id, name = "PK_Items_ID")
}