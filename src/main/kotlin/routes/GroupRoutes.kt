package ru.packet.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import ru.packet.dto.CreateGroupRequest
import ru.packet.dto.GroupDTO
import ru.packet.dto.GroupSummaryDTO
import ru.packet.models.Activities
import ru.packet.models.Groups

import ru.packet.services.GroupService

@kotlinx.serialization.Serializable
data class JoinGroupRequest(val userId: Int, val inviteCode: String)

fun Route.groupRoutes(groupService: GroupService) {
    route("/groups") {
        post {
            val groupRequest = call.receive<CreateGroupRequest>()
            println(groupRequest)
            val groupDTO = groupService.createGroup(
                name = groupRequest.name,
                creatorId = groupRequest.creatorId,
                isPersonal = groupRequest.isPersonal
            )
            call.respond(HttpStatusCode.Created, groupDTO)
        }

        post("/join") {
            val request = call.receive<JoinGroupRequest>()
            val userId = request.userId
            val inviteCode = request.inviteCode

            try {
                val groupDTO = groupService.joinGroup(
                    userId = userId,
                    inviteCode = inviteCode
                )
                call.respond(HttpStatusCode.OK, groupDTO)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Произошла ошибка на сервере"))
            }
        }

        authenticate("auth-jwt") {
            get("/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val user_Id = principal?.payload?.getClaim("userId")?.asInt()
                if (user_Id == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Недействительный токен"))
                    return@get
                }
                try{
                    val userId = call.parameters["userId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid userId")

                    val groupsDTO = groupService.getUserGroups(userId)
                    call.respond(groupsDTO)
                }catch (e: Exception) {
                    logger.error("Error getting groups of user: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Ошибка сервера: ${e.message}"))
                }

            }

            get("/summaries") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Недействительный токен"))
                    return@get
                }

                try {
                    val groups = groupService.getUserGroups(userId)
                    val groupIds = groups.map { it.id }
                    val summaries = groupService.getGroupSummaries(userId, groupIds)
                    call.respond(HttpStatusCode.OK, summaries)
                } catch (e: Exception) {
                    logger.error("Error fetching group summaries: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Ошибка сервера: ${e.message}"))
                }
            }

            get("/{groupId}/invite-code") {
                val principal = call.principal<JWTPrincipal>()
                val user_Id = principal?.payload?.getClaim("userId")?.asInt()
                if (user_Id == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Недействительный токен"))
                    return@get
                }
                try{
                    val groupId = call.parameters["groupId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid groupID")

                    val inviteCode = groupService.getInviteCode(groupId)
                    if (inviteCode == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Инвайт-код не найден"))
                    } else {
                        call.respond(inviteCode)
                    }
                }catch (e: Exception) {
                    logger.error("Error getting groupinvite code: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Ошибка сервера: ${e.message}"))
                }

            }

        }
    }
}
