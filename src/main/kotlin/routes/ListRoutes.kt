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
import ru.packet.services.GroupService
import ru.packet.services.ListService


fun Route.listRoutes(listService: ListService, groupService: GroupService) {
    // Авторизация через JWT
    authenticate("auth-jwt") {
//        route("/group-list-items") {
//            // Добавление товара в список группы
//            post {
//                val principal = call.principal<JWTPrincipal>()
//                val userId = principal?.payload?.getClaim("userId")?.asInt()
//                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
//
//                val listItem = call.receive<GroupListItemDTO>()
//                try {
//                    val newItem = listService.addItemToGroupList(
//                        groupId = listItem.groupId,
//                        itemId = listItem.itemId,
//                        quantity = listItem.quantity,
//                        priority = listItem.priority,
//                        budget = listItem.budget
//                    )
//                    call.respond(newItem)
//                } catch (e: Exception) {
//                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Failed to add item")
//                }
//            }
//
//        }
    }
}


