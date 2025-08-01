package ru.packet.services

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.mindrot.jbcrypt.BCrypt
import ru.packet.dto.ItemDTO
import ru.packet.dto.UserDTO
import ru.packet.models.Items
import ru.packet.models.Users

class UserService(private val database: Database) {
    private val resetCodes = mutableMapOf<Int, String>()

    fun registerUser(name: String, email: String, password: String, role: String): UserDTO {
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        return transaction {
            val userId = Users.insert {
                it[Users.name] = name
                it[Users.email] = email
                it[Users.passwordHash] = hashedPassword
                it[Users.role] = role
                it[Users.createdAt] = DateTime.now(DateTimeZone.UTC)
            }[Users.id]

            val user = Users.select { Users.id eq userId }.first()
            UserDTO(
                id = user[Users.id],
                name = user[Users.name],
                email = user[Users.email],
                role = user[Users.role],
                createdAt = user[Users.createdAt].toString()
            )
        }
    }

    fun loginUser(email: String, password: String): UserDTO? {
        return transaction {
            val user = Users.select { Users.email.lowerCase() eq email.lowercase() }.singleOrNull()
            if (user == null || !BCrypt.checkpw(password, user[Users.passwordHash])) {
                println("Login failed for email $email: user not found or invalid password")
                return@transaction null
            }

            UserDTO(
                id = user[Users.id],
                name = user[Users.name],
                email = user[Users.email],
                role = user[Users.role],
                createdAt = user[Users.createdAt].toString()
            )
        }
    }

    fun getAllUsers(): List<UserDTO> {
        return transaction {
            Users.selectAll().map {
                UserDTO(
                    id = it[Users.id],
                    name = it[Users.name],
                    email = it[Users.email],
                    role = it[Users.role],
                    createdAt = it[Users.createdAt].toString()
                )
            }
        }
    }

    fun findUserByEmail(email: String): UserDTO? {
        println("Looking for user with email: $email")
        val user = transaction {
            val users = Users.select { Users.email.lowerCase() eq email.lowercase() }.toList()
            println("Found ${users.size} users with email $email")
            users.forEach { println("User: ${it[Users.email]}, id=${it[Users.id]}") }
            users.singleOrNull()?.let {
                UserDTO(
                    id = it[Users.id],
                    name = it[Users.name],
                    email = it[Users.email],
                    role = it[Users.role],
                    createdAt = it[Users.createdAt].toString()
                )
            }
        }
        println("Returning user: $user")
        return user
    }

    fun findUserById(id: Int): UserDTO? {
        return transaction {
            Users.select { Users.id eq id }
                .singleOrNull()?.let {
                    UserDTO(
                        id = it[Users.id],
                        name = it[Users.name],
                        email = it[Users.email],
                        role = it[Users.role],
                        createdAt = it[Users.createdAt].toString()
                    )
                }
        }
    }

    suspend fun saveResetCode(userId: Int, code: String) {
        resetCodes[userId] = code
    }

    suspend fun verifyResetCode(code: String): Int? {
        return resetCodes.entries.find { it.value == code }?.key
    }

    suspend fun updateUserPassword(userId: Int, newPassword: String) {
        val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        transaction {

            Users.update({ Users.id eq userId }) {
                it[passwordHash] = hashedPassword
            }
        }
    }

    suspend fun clearResetCode(userId: Int) {
        resetCodes.remove(userId)
    }

    fun updateUser(userId: Int, name: String, email: String, password: String?): UserDTO? {
        return transaction {
            try {
                Users.update({ Users.id eq userId }) { update ->
                    update[Users.name] = name
                    update[Users.email] = email
                    if (password != null) {
                        update[Users.passwordHash] = BCrypt.hashpw(password, BCrypt.gensalt())
                    }
                }
                findUserById(userId)
            } catch (e: ExposedSQLException) {
                if (e.cause?.message?.contains("users_email_key") == true) {
                    throw Exception("Пользователь с таким email уже существует")
                } else {
                    throw Exception("Ошибка сервера: ${e.message}")
                }
            }
        }
    }


}


