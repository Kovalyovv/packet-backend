package ru.packet.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import ru.packet.dto.GroupListItemDTO
import ru.packet.dto.GroupSummaryDTO
import ru.packet.services.ActivityService
import ru.packet.services.GroupService
import kotlinx.serialization.Serializable
import ru.packet.dto.BuyItemRequest

fun Route.activityRoutes(activityService: ActivityService, groupService: GroupService) {
    val logger = LoggerFactory.getLogger("ActivityRoutes")

    authenticate("auth-jwt") {
        route("/groups") {

            get("/{groupId}/items") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val groupId = call.parameters["groupId"]?.toIntOrNull()
                if (userId == null || groupId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный запрос"))
                    return@get
                }

                val groups = groupService.getUserGroups(userId)
                if (groups.none { it.id == groupId }) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Доступ к группе запрещён"))
                    return@get
                }

                val items = activityService.getGroupItems(groupId)
                call.respond(items)
            }

            post("/{groupId}/items") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val groupId = call.parameters["groupId"]?.toIntOrNull()
                if (userId == null || groupId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный запрос"))
                    return@post
                }

                val groups = groupService.getUserGroups(userId)
                if (groups.none { it.id == groupId }) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Доступ к группе запрещён"))
                    return@post
                }

                try {
                    val request = call.receive<AddItemRequest>()
                    val item = activityService.addItemToGroupList(
                        groupId = groupId,
                        userId = userId,
                        itemId = request.itemId,
                        quantity = request.quantity,
                        priority = request.priority,
                        budget = request.budget
                    )
                    call.respond(HttpStatusCode.Created, item)
                } catch (e: Exception) {
                    logger.error("Add item error: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Ошибка добавления товара: ${e.message}"))
                }
            }
            post("/{groupId}/items") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val groupId = call.parameters["groupId"]?.toIntOrNull()
                if (userId == null || groupId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный запрос"))
                    return@post
                }

                val groups = groupService.getUserGroups(userId)
                if (groups.none { it.id == groupId }) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Доступ к группе запрещён"))
                    return@post
                }

                try {
                    val request = call.receive<AddItemRequest>()
                    val item = activityService.addItemToGroupList(
                        groupId = groupId,
                        userId = userId,
                        itemId = request.itemId,
                        quantity = request.quantity,
                        priority = request.priority,
                        budget = request.budget
                    )
                    call.respond(HttpStatusCode.Created, item)
                } catch (e: Exception) {
                    logger.error("Add item error: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Ошибка добавления товара: ${e.message}"))
                }
            }

            post("/{groupId}/items/{itemId}/buy") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val groupId = call.parameters["groupId"]?.toIntOrNull()
                val itemId = call.parameters["itemId"]?.toIntOrNull()
                if (userId == null || groupId == null || itemId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный запрос"))
                    return@post
                }

                val groups = groupService.getUserGroups(userId)
                if (groups.none { it.id == groupId }) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Доступ к группе запрещён"))
                    return@post
                }

                try {
                    val request = call.receive<BuyItemRequest>()
                    if (request.boughtBy != userId) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Недостаточно прав для покупки"))
                        return@post
                    }
                    activityService.buyItem(itemId, request)
                    call.respond(HttpStatusCode.OK, SuccessResponse("Товар куплен"))
                } catch (e: Exception) {
                    logger.error("Buy item error: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Ошибка покупки товара: ${e.message}"))
                }
            }

            post("/{groupId}/mark-viewed") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val groupId = call.parameters["groupId"]?.toIntOrNull()
                if (userId == null || groupId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный запрос"))
                    return@post
                }

                val groups = groupService.getUserGroups(userId)
                if (groups.none { it.id == groupId }) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Доступ к группе запрещён"))
                    return@post
                }

                val request = call.receive<MarkViewedRequest>()
                activityService.markItemsAsViewed(groupId, request.itemIds)
                call.respond(HttpStatusCode.OK, SuccessResponse("Товары отмечены как просмотренные"))
            }

            post("/{groupId}/mark-all-viewed") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val groupId = call.parameters["groupId"]?.toIntOrNull()
                if (userId == null || groupId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Неверный запрос"))
                    return@post
                }

                val groups = groupService.getUserGroups(userId)
                if (groups.none { it.id == groupId }) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Доступ к группе запрещён"))
                    return@post
                }

                activityService.markAllActivitiesAsViewed(groupId)
                call.respond(HttpStatusCode.OK, SuccessResponse("Все активности отмечены как просмотренные"))
            }
        }
    }
}

@Serializable
data class AddItemRequest(
    val itemId: Int,
    val quantity: Int,
    val priority: Int,
    val budget: Int?
)

@Serializable
data class MarkViewedRequest(val itemIds: List<Int>)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class SuccessResponse(val message: String)