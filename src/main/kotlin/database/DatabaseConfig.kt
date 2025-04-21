package ru.packet.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import ru.packet.models.*

object DatabaseConfig {
    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)

    fun init(): Database {
        try {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:postgresql://localhost:5432/packet_db"
                driverClassName = "org.postgresql.Driver"
                username = "postgres"
                password = "123"
                maximumPoolSize = 10
                minimumIdle = 2
                idleTimeout = 30000
                connectionTimeout = 30000
                maxLifetime = 1800000
            }

            val dataSource = HikariDataSource(config)
            val database = Database.connect(dataSource)

            transaction(database) {
                SchemaUtils.create(
                    Users, Groups, GroupMembers, Items,
                    GroupListItems,
                    Receipts, ReceiptItems, ChatMessages, Activities
                )
            }

            logger.info("Database initialized successfully")
            return database
        } catch (e: Exception) {
            logger.error("Failed to initialize database: ${e.message}", e)
            throw e
        }
    }
}