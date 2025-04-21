package ru.packet.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import ru.packet.dto.ChatMessageDTO
import ru.packet.services.ChatService
import java.util.concurrent.ConcurrentHashMap

fun Route.chatRoutes(
    chatConnections: ConcurrentHashMap<Int, MutableList<WebSocketSession>>,
    chatService: ChatService
) {
    route("/chat") {
        // Получение истории сообщений
        get("/{groupId}/messages") {
            val groupId = call.parameters["groupId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid groupId")
            val messages = chatService.getMessagesByGroupId(groupId)
            call.respond(messages)
        }

        // WebSocket для чата
        webSocket("/{groupId}") {
            val groupId = call.parameters["groupId"]?.toIntOrNull()
                ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing groupId"))

            val connections = chatConnections.getOrPut(groupId) { mutableListOf() }
            connections.add(this)

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val messageText = frame.readText()
                        val message = Json.decodeFromString(ChatMessageDTO.serializer(), messageText)

                        // Сохраняем сообщение в базе
                        val savedMessage = chatService.saveMessage(
                            groupId = message.groupId,
                            senderId = message.senderId,
                            text = message.text,
                            timestamp = message.timestamp
                        )

                        // Отправляем обновлённое сообщение всем участникам группы
                        val updatedMessageText = Json.encodeToString(ChatMessageDTO.serializer(), savedMessage)
                        connections.forEach { it.send(Frame.Text(updatedMessageText)) }
                    }
                }
            } finally {
                connections.remove(this)
            }
        }
    }
}