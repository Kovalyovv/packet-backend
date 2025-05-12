package ru.packet.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.packet.dto.ErrorResponse
import ru.packet.dto.ItemDTO
import ru.packet.services.ItemService

fun Route.itemRoutes(itemService: ItemService) {
    route("/items") {
        authenticate("auth-jwt") {
            get("/search") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val query = call.parameters["query"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("query отсутствует"))
                val items = itemService.searchItems(query)
                call.respond(items)
            }

            post {
                try {
                    val item = call.receive<ItemDTO>()
                    val newItem = itemService.createItem(
                        name = item.name,
                        barcode = item.barcode,
                        category = item.category,
                        price = item.price
                    )
                    call.respond(newItem)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                }
            }

            get {
                val items = itemService.getAllItems()
                call.respond(items)
            }
            get("/{itemId}") {

                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing barcode")
                try {
                    val item = itemService.getItemById(itemId)
                    if (item == null) {
                        call.respond(HttpStatusCode.NotFound, "Item not found")
                    } else {
                        call.respond(item)
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid itemId")
                }
            }

            get("/barcode/{barcode}") {
                val barcode = call.parameters["barcode"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing barcode")
                try {
                    val item = itemService.getItemByBarcode(barcode)
                    if (item == null) {
                        call.respond(HttpStatusCode.NotFound, "Item not found")
                    } else {
                        call.respond(item)
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid barcode")
                }
            }

        }

    }
}