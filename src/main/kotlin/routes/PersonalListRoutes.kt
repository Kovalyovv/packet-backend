import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import ru.packet.auth.JwtConfig
import ru.packet.dto.*
import ru.packet.services.ItemService
import ru.packet.services.PersonalListService

fun Route.personalListRoutes(itemService: ItemService, personalListService: PersonalListService) {
    route("/personal-list") {
        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val personalList = personalListService.getPersonalList(userId)
                call.respond(personalList)
            }

            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val request = call.receive<Map<String, Int>>()
                val itemId = request["itemId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("itemId отсутствует")
                )
                val quantity = request["quantity"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("quantity отсутствует")
                )

                val item = itemService.getItemById(itemId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Товар не найден"))

                val newItem = personalListService.addItemToPersonalList(userId, itemId, item.name, quantity)
                call.respond(HttpStatusCode.Created, newItem)
            }

            // Маршрут для пометки товара как купленного
            post("/{id}/mark-purchased") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val itemId = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("id отсутствует"))

                val request = call.receive<Map<String, Int>>()
                val price = request["price"] ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("price отсутствует"))

                val success = personalListService.markAsPurchased(userId, itemId, price)
                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Товар помечен как купленный"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Элемент не найден"))
                }
            }
        }
    }
    route("/personal-purchase-history") {
        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val history = personalListService.getPurchaseHistory(userId)
                call.respond(history)
            }
        }
    }


}

