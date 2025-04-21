package ru.packet.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.packet.dto.BuyItemRequest
import ru.packet.dto.GroupListItemDTO
import ru.packet.services.ListService


fun Route.listRoutes(listService: ListService) {
    // Авторизация через JWT
    authenticate("auth-jwt") {
        route("/group-list-items") {
            // Добавление товара в список группы
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")

                val listItem = call.receive<GroupListItemDTO>()
                try {
                    val newItem = listService.addItemToGroupList(
                        groupId = listItem.groupId,
                        itemId = listItem.itemId,
                        quantity = listItem.quantity,
                        priority = listItem.priority,
                        budget = listItem.budget
                    )
                    call.respond(newItem)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Failed to add item")
                }
            }

            // Получение товаров в списке группы
            get("/{groupId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")

                val groupId = call.parameters["groupId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid groupId")

                val items = listService.getItemsByGroupId(groupId)
                call.respond(items)
            }

            // Пометка товара как купленного
            put("/{itemId}/buy") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")

                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid itemId")

                val update = call.receive<BuyItemRequest>()

                // Проверка: пользователь, который покупает (boughtBy), должен совпадать с userId из токена
                if (update.boughtBy != userId) {
                    return@put call.respond(HttpStatusCode.Forbidden, "You can only mark items as bought for yourself")
                }

                try {
                    listService.markItemAsBought(itemId, update)
                    call.respond(HttpStatusCode.OK, "Item marked as bought")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Failed to mark item as bought")
                }
            }
        }
    }
}


