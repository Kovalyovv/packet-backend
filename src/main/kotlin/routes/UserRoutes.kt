package ru.packet.routes

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

import ru.packet.services.UserService

import java.util.*

val logger = LoggerFactory.getLogger("UserRoutes")

fun Route.userRoutes(userService: UserService) {

    val logger = LoggerFactory.getLogger("AuthRoutes")
    route("/users") {
        post("/register") {
            try {
                val userRequest = call.receive<UserRequest>()
                logger.info("Register attempt with credentials: $userRequest")

                // Регистрируем пользователя
                val userDTO = userService.registerUser(
                    name = userRequest.name,
                    email = userRequest.email,
                    password = userRequest.password,
                    role = if (userRequest.role.isEmpty()) "standard" else userRequest.role
                )

                // Генерируем токены
                val tokenPair = JwtConfig.generateTokenPair(userDTO.id)

                // Возвращаем LoginResponse
                call.respond(
                    HttpStatusCode.Created,
                    LoginResponse(
                        token = tokenPair.accessToken,
                        refreshToken = tokenPair.refreshToken,
                        user = userDTO
                    )
                )
            } catch (e: ExposedSQLException) {
                if (e.cause is PSQLException && e.cause?.message?.contains("users_email_key") == true) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponse("Пользователь с таким email уже существует")
                    )
                } else {
                    logger.error("Registration error: ${e.message}", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Ошибка сервера: ${e.message}")
                    )
                }
            } catch (e: Exception) {
                logger.error("Registration error: ${e.message}", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Неверный запрос: ${e.message}")
                )
            }
        }

        post("/login") {
            val credentials = call.receive<UserLoginRequest>()
            logger.info("Login attempt with credentials: $credentials")
            val userDTO = userService.loginUser(credentials.email, credentials.password)
            if (userDTO == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
            } else {
                val tokenPair = JwtConfig.generateTokenPair(userDTO.id)
                call.respond(
                    LoginResponse(
                        token = tokenPair.accessToken,
                        refreshToken = tokenPair.refreshToken,
                        user = userDTO
                    )
                )
            }
        }

        post("/refresh-token") {
            try {
                val request = call.receive<RefreshTokenRequest>()
                val refreshToken = request.refreshToken
                val verifier = JwtConfig.makeVerifier()
                val decodedJWT = verifier.verify(refreshToken)
                val userId = decodedJWT.getClaim("userId").asInt()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Недействительный refresh-токен"))
                    return@post
                }
                val user = userService.findUserById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Пользователь не найден"))
                    return@post
                }
                val tokenPair = JwtConfig.generateTokenPair(userId)
                call.respond(
                    LoginResponse(
                        token = tokenPair.accessToken,
                        refreshToken = tokenPair.refreshToken,
                        user = user
                    )
                )
            } catch (e: Exception) {
                logger.error("Refresh token error: ${e.message}", e)
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Ошибка обновления токена: ${e.message}"))
            }
        }


        get {
            val users = userService.getAllUsers()
            call.respond(users)
        }


        post("/forgot-password") {
            try {
                val request = call.receive<ForgotPasswordRequest>()
                logger.info("Forgot password attempt for email: ${request.email}")

                val user = transaction {
                    userService.findUserByEmail(request.email)
                }

                if (user == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("Пользователь с таким email не найден")
                    )
                    return@post
                }

                // Генерируем код восстановления (6 цифр)
                val resetCode = (100000..999999).random().toString()

                // Сохраняем код в памяти (в продакшене — в базе данных с таймером)
                userService.saveResetCode(user.id, resetCode)

                // Выводим код в консоль (вместо отправки email для теста)
                println("Код восстановления для ${request.email}: $resetCode")

                call.respond(
                    HttpStatusCode.OK,
                    SuccessResponse("Код восстановления отправлен")
                )
            } catch (e: Exception) {
                logger.error("Forgot password error: ${e.message}", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Ошибка запроса: ${e.message}")
                )
            }
        }

        post("/reset-password") {
            try {
                val request = call.receive<ResetPasswordRequest>()
                logger.info("Reset password attempt with code: ${request.code}")

                val userId = userService.verifyResetCode(request.code)
                if (userId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Неверный или истёкший код восстановления")
                    )
                    return@post
                }

                userService.updateUserPassword(userId, request.newPassword)
                userService.clearResetCode(userId)

                call.respond(
                    HttpStatusCode.OK,
                    SuccessResponse("Пароль успешно обновлён")
                )
            } catch (e: Exception) {
                logger.error("Reset password error: ${e.message}", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Ошибка запроса: ${e.message}")
                )
            }
        }


        authenticate("auth-jwt") {
            get("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Недействительный токен"))
                    return@get
                }

                val user = coroutineScope {
                    transaction {
                        userService.findUserById(userId)
                    }
                }

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Пользователь не найден"))
                    return@get
                }

                call.respond(user)
            }
        }

    }
}

