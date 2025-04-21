package ru.packet.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import ru.packet.dto.ReceiptDTO
import ru.packet.dto.ReceiptItemDTO

import ru.packet.services.ReceiptService

fun Route.receiptRoutes(receiptService: ReceiptService) {
    route("/receipts") {

        // Сохранение чека
        post {
            try {
                val receipt = call.receive<ReceiptDTO>()
                val savedReceipt = receiptService.saveReceipt(
                    userId = receipt.userId,
                    groupId = receipt.groupId,
                    qrCode = receipt.qrCode,
                    totalAmount = receipt.totalAmount,
                    scannedAt = receipt.scannedAt
                )
                call.respond(HttpStatusCode.Created, mapOf("receiptId" to savedReceipt.id))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
            }
        }

        // Добавление товаров из чека
        post("/{receiptId}/items") {
            val receiptId = call.parameters["receiptId"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid receiptId")
            try {
                val items = call.receive<List<ReceiptItemDTO>>()
                receiptService.addItemsToReceipt(receiptId, items)
                call.respond(HttpStatusCode.OK, "Items added")
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
            }
        }

        // Сопоставление товаров из чека с товарами из списка
        post("/{receiptId}/match") {
            val receiptId = call.parameters["receiptId"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid receiptId")
            try {
                receiptService.matchReceiptItems(receiptId)
                call.respond(HttpStatusCode.OK, "Matching completed")
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
            }
        }

        // Перенос данных в историю покупок
        post("/{receiptId}/transfer") {
            val receiptId = call.parameters["receiptId"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid receiptId")
            try {
                receiptService.transferReceiptItemsToHistory(receiptId)
                call.respond(HttpStatusCode.OK, "Transfer completed")
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
            }
        }
    }
}

