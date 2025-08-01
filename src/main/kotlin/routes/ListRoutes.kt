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
    }
}


