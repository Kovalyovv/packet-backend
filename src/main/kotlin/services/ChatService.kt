package ru.packet.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import ru.packet.dto.ChatMessageDTO
import ru.packet.models.ChatMessages
import java.util.concurrent.ConcurrentHashMap

class ChatService(private val database: Database) {
    private val tempIdMap = ConcurrentHashMap<Int, Int>()

    fun getMessagesByGroupId(groupId: Int): List<ChatMessageDTO> {
        if (groupId <= 0) throw IllegalArgumentException("Invalid groupId")
        return transaction {
            val messages = ChatMessages.select { ChatMessages.groupId eq groupId }
                .orderBy(ChatMessages.timestamp to SortOrder.ASC)
                .map {
                    val dto = ChatMessageDTO(
                        id = it[ChatMessages.id],
                        groupId = it[ChatMessages.groupId],
                        senderId = it[ChatMessages.senderId],
                        text = it[ChatMessages.text],
                        timestamp = it[ChatMessages.timestamp].toString(),
                        replyToId = it[ChatMessages.replyToId]
                    )
                    println("ChatService: Sending message: $dto")
                    dto
                }
            println("ChatService: Returning ${messages.size} messages for group $groupId")
            messages
        }
    }

    fun saveMessage(
        groupId: Int,
        senderId: Int,
        text: String,
        timestamp: String,
        replyToId: Int? = null,
        tempId: Int? = null
    ): ChatMessageDTO {
        if (groupId <= 0) throw IllegalArgumentException("Invalid groupId")
        if (senderId <= 0) throw IllegalArgumentException("Invalid senderId")
        if (text.isBlank()) throw IllegalArgumentException("Message text cannot be blank")

        return transaction {
            val validReplyToId = replyToId?.let { id ->
                if (id < 0) {
                    tempIdMap[id]?.takeIf { permanentId ->
                        ChatMessages.select { ChatMessages.id eq permanentId }.count() > 0
                    }
                } else {
                    if (ChatMessages.select { ChatMessages.id eq id }.count() > 0) id else null
                }
            }

            val messageId = ChatMessages.insert {
                it[ChatMessages.groupId] = groupId
                it[ChatMessages.senderId] = senderId
                it[ChatMessages.text] = text
                it[ChatMessages.timestamp] = DateTime.parse(timestamp)
                it[ChatMessages.replyToId] = validReplyToId
            }[ChatMessages.id]

            if (tempId != null && tempId <= 0) {
                tempIdMap[tempId] = messageId
                println("ChatService: Mapped tempId=$tempId to permanentId=$messageId")
            }

            val savedMessage = ChatMessageDTO(
                id = messageId,
                groupId = groupId,
                senderId = senderId,
                text = text,
                timestamp = timestamp,
                replyToId = validReplyToId
            )
            println("ChatService: Saved message: $savedMessage")
            savedMessage
        }
    }

    fun deleteMessage(messageId: Int) {
        if (messageId <= 0) throw IllegalArgumentException("Invalid messageId")
        transaction {
            ChatMessages.deleteWhere { ChatMessages.id eq messageId }
        }
    }

    fun clearTempIdMap() {
        tempIdMap.clear()
    }
}