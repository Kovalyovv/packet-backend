package ru.packet

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import io.ktor.server.engine.*
import io.ktor.server.netty.*

import io.ktor.server.websocket.*
import io.ktor.websocket.*

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import ru.packet.models.ChatMessage
import ru.packet.models.ChatMessages
import java.util.concurrent.ConcurrentHashMap
import ru.packet.routes.groupRoutes
import ru.packet.routes.itemRoutes
import ru.packet.routes.listRoutes
import ru.packet.routes.receiptRoutes
import ru.packet.routes.userRoutes
import ru.packet.routes.chatRoutes

import kotlinx.serialization.Serializable


@Serializable
data class Text(
    val text: String
)


