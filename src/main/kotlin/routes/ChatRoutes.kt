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
    get("/{groupId}/messages") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid groupId")
        val messages = chatService.getMessagesByGroupId(groupId)
        call.respond(messages)
    }

    delete("/{messageId}") {
        val messageId = call.parameters["messageId"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid messageId")
        chatService.deleteMessage(messageId)
        call.respond(HttpStatusCode.OK, "Message deleted")
    }

    webSocket("/{groupId}") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
            ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing groupId"))

        println("WebSocket connected for group $groupId")
        val connections = chatConnections.getOrPut(groupId) { mutableListOf() }
        connections.add(this)

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val messageText = frame.readText()
                    println("Received message: $messageText")
                    val message = Json.decodeFromString(ChatMessageDTO.serializer(), messageText)

                    val savedMessage = chatService.saveMessage(
                        groupId = message.groupId,
                        senderId = message.senderId,
                        text = message.text,
                        timestamp = message.timestamp,
                        replyToId = message.replyToId,
                        tempId = message.id
                    )
                    println("Saved message: $savedMessage")

                    val updatedMessageText = Json.encodeToString(ChatMessageDTO.serializer(), savedMessage)
                    connections.forEach {
                        it.send(Frame.Text(updatedMessageText))
                        println("Sent message to connection: $updatedMessageText")
                    }
                }
            }
        } finally {
            connections.remove(this)
            println("WebSocket disconnected for group $groupId")
        }
    }
}