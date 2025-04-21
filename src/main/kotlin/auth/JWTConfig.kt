package ru.packet.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

object JwtConfig {
    private const val SECRET = "packet_secret" // Замените на свой секретный ключ
    private const val ISSUER = "http://localhost:8080/"
    private const val AUDIENCE = "http://localhost:8080/"
    const val REALM = "Access to protected routes"

    // Время жизни access token (5 минут)
    private const val ACCESS_TOKEN_VALIDITY_MS = 5 * 60 * 1000L
    // Время жизни refresh token (30 дней)
    private const val REFRESH_TOKEN_VALIDITY_MS = 30 * 24 * 60 * 60 * 1000L

    data class TokenPair(val accessToken: String, val refreshToken: String)


    fun generateTokenPair(userId: Int): TokenPair {
        val accessToken = JWT.create()
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY_MS))
            .sign(Algorithm.HMAC256(SECRET))

        val refreshToken = JWT.create()
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY_MS))
            .sign(Algorithm.HMAC256(SECRET))

        return TokenPair(accessToken, refreshToken)
    }

    fun configureJwt(verifier: JWTVerifier): JWTAuthenticationProvider.Config.() -> Unit = {
        realm = REALM
        this.verifier(verifier)
        validate { credential ->
            credential.payload.getClaim("userId")?.asInt()?.let { userId ->
                JWTPrincipal(credential.payload)
            }
        }
    }

    fun makeVerifier() = JWT
        .require(Algorithm.HMAC256(SECRET))
        .withAudience(AUDIENCE)
        .withIssuer(ISSUER)
        .build()

}