package ru.packet.services

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
                .mapNotNull {
                    UserDTO(
                        id = it[Users.id],
                        name = it[Users.name],
                        email = it[Users.email],
                        role = it[Users.role],
                        createdAt = it[Users.createdAt].toString()
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun saveResetCode(userId: Int, code: String) {
        resetCodes[userId] = code
        // В продакшене: сохранить в БД с таймером истечения
    }

    suspend fun verifyResetCode(code: String): Int? {
        return resetCodes.entries.find { it.value == code }?.key
        // В продакшене: проверить код в БД и его срок действия
    }

    suspend fun updateUserPassword(userId: Int, newPassword: String) {
        val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        transaction {

            Users.update({ Users.id eq userId }) {
                it[passwordHash] = hashedPassword // Предполагается, что пароль уже хэширован
            }
        }
    }

    suspend fun clearResetCode(userId: Int) {
        resetCodes.remove(userId)
        // В продакшене: удалить код из БД
    }


}


