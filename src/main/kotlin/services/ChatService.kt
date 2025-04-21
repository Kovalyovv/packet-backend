package ru.packet.services

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import ru.packet.dto.ChatMessageDTO
import ru.packet.models.ChatMessages

class ChatService(private val database: Database) {
    fun getMessagesByGroupId(groupId: Int): List<ChatMessageDTO> {
        if (groupId <= 0) throw IllegalArgumentException("Invalid groupId")

        return transaction {
            ChatMessages.select { ChatMessages.groupId eq groupId }
                .orderBy(ChatMessages.timestamp to SortOrder.ASC)
                .map {
                    ChatMessageDTO(
                        id = it[ChatMessages.id],
                        groupId = it[ChatMessages.groupId],
                        senderId = it[ChatMessages.senderId],
                        text = it[ChatMessages.text],
                        timestamp = it[ChatMessages.timestamp].toString()
                    )
                }
        }
    }

    fun saveMessage(groupId: Int, senderId: Int, text: String, timestamp: String): ChatMessageDTO {
        if (groupId <= 0) throw IllegalArgumentException("Invalid groupId")
        if (senderId <= 0) throw IllegalArgumentException("Invalid senderId")
        if (text.isBlank()) throw IllegalArgumentException("Message text cannot be blank")

        return transaction {
            val messageId = ChatMessages.insert {
                it[ChatMessages.groupId] = groupId
                it[ChatMessages.senderId] = senderId
                it[ChatMessages.text] = text
                it[ChatMessages.timestamp] = java.time.Instant.parse(timestamp)
            }[ChatMessages.id]

            ChatMessageDTO(
                id = messageId,
                groupId = groupId,
                senderId = senderId,
                text = text,
                timestamp = timestamp
            )
        }
    }
}