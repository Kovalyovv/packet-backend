package ru.packet.routes

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.joda.time.DateTime
import ru.packet.dto.*
import ru.packet.dto.ErrorResponse
import ru.packet.services.NalogReceiptResponse
import ru.packet.services.ReceiptService
import java.sql.SQLIntegrityConstraintViolationException
import kotlin.math.ceil

@Serializable
data class ScanReceiptResponse(
    val first: ReceiptDTO,
    val second: ProcessedCheckData,
    val message: String? = null
)

fun Route.receiptRoutes(receiptService: ReceiptService, client: HttpClient) {
    authenticate("auth-jwt") {
        route("/receipts") {

            post("/scan") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))
                val params = call.receiveParameters()

                val qrCode = params["qrCode"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "QR code is required")

                try {
                    println("scan with qr $qrCode")
                    val response: HttpResponse = client.post("https://proverkacheka.com/api/v1/check/get") {
                        contentType(ContentType.Application.FormUrlEncoded)
                        setBody(
                            FormDataContent(Parameters.build {
                                append("qrraw", qrCode)
                                append("qr", "3")
                                append("token", "32673.gd5quFNqWvdCFHgpG")
                            })
                        )
                    }
                    println("response: $response")
                    val receiptData: NalogReceiptResponse = when (response.status) {
                        HttpStatusCode.OK -> response.body()
                        else -> throw IllegalStateException("Ошибка получения данных чека: ${response.status.description}")
                    }
                    println("receiptData: $receiptData")
                    if (receiptData.code != 1) {
                        throw IllegalStateException("Ошибка API proverkacheka.com: код ${receiptData.code}")
                    }

                    val checkJson = receiptData.data?.json ?: throw IllegalStateException("Данные чека отсутствуют")

                    val totalSumInRubles = checkJson.totalSum?.div(100) ?: 0.0
                    val roundedTotalSum = ceil(totalSumInRubles).toInt()

                    val processedItems = checkJson.items?.map { item ->
                        val priceInRubles = item.price.div(100)
                        val roundedPrice = ceil(priceInRubles).toInt()
                        ProcessedCheckItem(
                            name = item.name,
                            price = roundedPrice,
                            quantity = item.quantity
                        )
                    } ?: emptyList()

                    val processedCheckData = ProcessedCheckData(
                        totalSum = roundedTotalSum,
                        items = processedItems
                    )

                    val result = receiptService.processQrCode(userId, qrCode, processedCheckData)
                    println("res = $result")
                    // Проверяем, принадлежит ли чек другому пользователю
                    if (result.first.userId != userId) {
                        call.respond(
                            HttpStatusCode.Conflict,
                            ScanReceiptResponse(
                                first = result.first,
                                second = result.second,
                                message = "Этот QR-код уже был отсканирован другим пользователем"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.OK,
                            ScanReceiptResponse(
                                first = result.first,
                                second = result.second,
                                message = if (result.first.scannedAt != DateTime.now().toString()) "QR-код уже был отсканирован ранее" else null
                            )
                        )
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Failed to fetch receipt data"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Ошибка обработки QR-кода: ${e.message}"))
                }
            }

            post("/{receiptId}/confirm") {
                val receiptId = call.parameters["receiptId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid receiptId")
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                val request = call.receive<ConfirmItemsRequest>()
                println("Received ConfirmItemsRequest: $request")

                try {
                    receiptService.confirmReceiptItems(receiptId, userId, request.groupId, request.items)
                    call.respond(HttpStatusCode.OK, "Items confirmed and processed")
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                }
            }

            post("/{receiptId}/update-group") {
                val receiptId = call.parameters["receiptId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid receiptId")
                val params = call.receiveParameters()
                val groupId = params["groupId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid groupId")

                try {
                    receiptService.updateReceiptGroup(receiptId, groupId)
                    call.respond(HttpStatusCode.OK, "Group updated")
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                }
            }

            post("/{receiptId}/items") {
                val receiptId = call.parameters["receiptId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid receiptId")
                try {
                    val items = call.receive<List<ReceiptItemDTO>>()
                    receiptService.addItemsToReceiptAndItems(receiptId, items)
                    call.respond(HttpStatusCode.OK, "Items added")
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                }
            }

            post("/{receiptId}/match") {
                val receiptId = call.parameters["receiptId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid receiptId")
                val params = call.receiveParameters()
                val isPersonalList = params["isPersonalList"]?.toBoolean() ?: false

                try {
                    receiptService.matchReceiptItems(receiptId, isPersonalList)
                    call.respond(HttpStatusCode.OK, "Matching completed")
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                }
            }

            post("/{receiptId}/transfer") {
                val receiptId = call.parameters["receiptId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid receiptId")
                val params = call.receiveParameters()
                val isPersonalList = params["isPersonalList"]?.toBoolean() ?: false

                try {
                    receiptService.transferReceiptItemsToHistory(receiptId, isPersonalList)
                    call.respond(HttpStatusCode.OK, "Transfer completed")
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                }
            }

            post("/history") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Токен недействителен"))

                try {
                    val history = receiptService.getReceiptsHistory(userId)
                    call.respond(HttpStatusCode.OK, history)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Failed to fetch receipt history: ${e.message}"))
                }
            }
        }
    }
}