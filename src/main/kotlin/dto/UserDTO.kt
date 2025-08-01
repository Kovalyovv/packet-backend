package ru.packet.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val createdAt: String
)


@Serializable
data class UserRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = ""
)


@Serializable
data class UserLoginRequest(
    val email: String,
    val password: String

)

@Serializable
data class ForgotPasswordRequest(val email: String)



@Serializable
data class SuccessResponse(val message: String)


@Serializable
data class ResetPasswordRequest(
    val code: String,
    val newPassword: String
)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)