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
            println("Authenticated /personal-list route")
            get {
                println("Handling GET /personal-list")
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val personalList = personalListService.getPersonalList(userId)
                call.respond(personalList)
            }
            get("/{userId}") {
                println("Handling GET /personal-list/{userId}")
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val personalList = personalListService.getPersonalListItems(userId)
                call.respond(personalList)
            }

            post {
                println("Handling POST /personal-list")
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val request = call.receive<AddItemRequest>()
                println("Received AddItemRequest: $request")
                val newItem = personalListService.addItemToPersonalList(
                    userId = userId,
                    itemId = request.itemId, // itemId теперь может быть null
                    itemName = request.itemName,
                    quantity = request.quantity,
                    priceItem = request.price
                )
                call.respond(HttpStatusCode.Created, newItem)
            }

            // Новый маршрут для массового добавления
            post("/add-items") {

                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val request = call.receive<List<AddItemRequest>>()
                request.forEach { item ->
                    personalListService.addItemToPersonalList(
                        userId = userId,
                        itemId = item.itemId,
                        itemName = item.itemName,
                        quantity = item.quantity,
                        priceItem = item.price
                    )
                }
                call.respond(HttpStatusCode.OK, "Items added successfully")
            }

            // Маршрут для пометки товара как купленного
            post("/{id}/mark-purchased") {
                println("Handling POST /personal-list/{id}/mark-purchased")
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val itemId = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("id отсутствует"))

                val success = personalListService.markAsPurchased(userId, itemId)
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

