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
import ru.packet.services.UserService
import java.util.concurrent.ConcurrentHashMap

fun Route.chatRoutes(
    chatConnections: ConcurrentHashMap<Int, MutableList<WebSocketSession>>,
    chatService: ChatService,
    userService: UserService,
) {
    get("/{groupId}/messages") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid groupId")
        val messages = chatService.getMessagesByGroupId(groupId)
        call.respond(messages)
    }

    delete("/{token}") {
        val token = call.parameters["token"]
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid token")
        chatService.deleteMessage(token)
        call.respond(HttpStatusCode.OK, "Message deleted")
    }
    get("/{groupId}/users") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid groupId")
        val userIds = chatService.getChatUserIds(groupId)
        if (userIds.isEmpty()) {
            return@get call.respond(HttpStatusCode.NotFound, "Chat or users not found")
        }
        val users = userIds.mapNotNull { userService.findUserById(it) }
        call.respond(users)
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
                        token = message.token,
                        groupId = message.groupId,
                        senderId = message.senderId,
                        text = message.text,
                        timestamp = message.timestamp,
                        replyToToken = message.replyToToken
                    )
                    println("Saved message: $savedMessage")

                    val updatedMessageText = Json.encodeToString(ChatMessageDTO.serializer(), savedMessage)
                    connections.forEach {
                        it.send(Frame.Text(updatedMessageText))
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