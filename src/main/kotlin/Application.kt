package ru.packet

import io.ktor.client.*
import io.ktor.http.*
import ru.packet.routes.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.response.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import org.jetbrains.exposed.sql.Database
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import personalListRoutes
import ru.packet.auth.JwtConfig

import ru.packet.di.appModule
import ru.packet.services.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureKoin()
        configureSerialization()
        configureDatabase()
        configureAuthentication()
        configureSockets()
        configureRouting()
        configureStatusPages()

    }.start(wait = true)
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid request"))
        }
        exception<Throwable> { call, cause ->
            // Логируем ошибку
            logger.error("Unhandled exception: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error: ${cause.message}"))
        }
    }
}

fun Application.configureKoin() {
    install(Koin) {
        modules(appModule)
    }
}

fun Application.configureAuthentication() {
    install(Authentication) {
        jwt("auth-jwt") {
            JwtConfig.configureJwt(JwtConfig.makeVerifier())()
        }
    }
}

fun Application.configureDatabase() {
    getKoin().get<Database>()
}

fun Application.configureRouting() {
    val chatConnections = ConcurrentHashMap<Int, MutableList<WebSocketSession>>()
    val koin = getKoin()

    routing {
        intercept(ApplicationCallPipeline.Call) {
            proceed()
        }
        userRoutes(koin.get())
        groupRoutes(koin.get())
        itemRoutes(koin.get())
        receiptRoutes(koin.get<ReceiptService>(), koin.get<HttpClient>())
        activityRoutes(koin.get<ActivityService>(), koin.get<GroupService>())
        personalListRoutes(koin.get<ItemService>(), koin.get<PersonalListService>())
        route("/chat") {
            authenticate("auth-jwt") {
                chatRoutes(chatConnections, koin.get<ChatService>(), koin.get<UserService>())
            }
        }
    }
}