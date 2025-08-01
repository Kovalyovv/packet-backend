package ru.packet.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import ru.packet.dto.ChatMessageDTO
import ru.packet.models.ChatMessages
import ru.packet.models.GroupMembers

class ChatService(private val database: Database) {
    fun getMessagesByGroupId(groupId: Int): List<ChatMessageDTO> {
        if (groupId <= 0) throw IllegalArgumentException("Invalid groupId")
        return transaction {
            val messages = ChatMessages.select { ChatMessages.groupId eq groupId }
                .orderBy(ChatMessages.timestamp to SortOrder.ASC)
                .map {
                    val dto = ChatMessageDTO(
                        token = it[ChatMessages.token],
                        groupId = it[ChatMessages.groupId],
                        senderId = it[ChatMessages.senderId],
                        text = it[ChatMessages.text],
                        timestamp = it[ChatMessages.timestamp].toString(),
                        replyToToken = it[ChatMessages.replyToToken]
                    )

                    dto
                }
            println("ChatService: Returning ${messages.size} messages for group $groupId")
            messages
        }
    }
    fun getChatUserIds(groupId: Int): List<Int> {
        if (groupId <= 0) throw IllegalArgumentException("Invalid groupId")
        return transaction {
            GroupMembers.select { GroupMembers.group eq groupId }
                .map { it[GroupMembers.user] }
                .distinct()
        }
    }

    fun saveMessage(
        token: String,
        groupId: Int,
        senderId: Int,
        text: String,
        timestamp: String,
        replyToToken: String? = null
    ): ChatMessageDTO {
        if (groupId <= 0) throw IllegalArgumentException("Invalid groupId")
        if (senderId <= 0) throw IllegalArgumentException("Invalid senderId")
        if (text.isBlank()) throw IllegalArgumentException("Message text cannot be blank")
        if (token.isBlank()) throw IllegalArgumentException("Token cannot be blank")

        return transaction {
            val validReplyToToken = replyToToken?.let { rToken ->
                if (ChatMessages.select { ChatMessages.token eq rToken }.count() > 0) rToken else null
            }

            if (ChatMessages.select { ChatMessages.token eq token }.count() > 0) {
                throw IllegalArgumentException("Token $token already exists")
            }

            ChatMessages.insert {
                it[ChatMessages.token] = token
                it[ChatMessages.groupId] = groupId
                it[ChatMessages.senderId] = senderId
                it[ChatMessages.text] = text
                it[ChatMessages.timestamp] = DateTime.parse(timestamp)
                it[ChatMessages.replyToToken] = validReplyToToken
            }

            val savedMessage = ChatMessageDTO(
                token = token,
                groupId = groupId,
                senderId = senderId,
                text = text,
                timestamp = timestamp,
                replyToToken = validReplyToToken
            )
            println("ChatService: Saved message: $savedMessage")
            savedMessage
        }
    }

    fun deleteMessage(token: String) {
        if (token.isBlank()) throw IllegalArgumentException("Invalid token")
        transaction {
            ChatMessages.deleteWhere { ChatMessages.token eq token }
        }
    }
}