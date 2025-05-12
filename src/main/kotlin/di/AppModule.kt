package ru.packet.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.* // Импортируем клиентский ContentNegotiation
import io.ktor.serialization.kotlinx.json.* // Для поддержки JSON
import org.koin.dsl.module
import org.jetbrains.exposed.sql.Database
import ru.packet.database.DatabaseConfig
import ru.packet.services.*
import kotlinx.serialization.json.Json // Для настройки JSON-сериализации

val appModule = module {
    single<Database> { DatabaseConfig.init() }
    single { UserService(get()) }
    single { GroupService(get()) }
    single { ActivityService(get()) }
    single { ItemService(get()) }
    single { ListService(get()) }
    single { PersonalListService() }
    single { ReceiptService(get()) }
    single { ChatService(get()) }

    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) { // Используем клиентский ContentNegotiation
                json(Json { // Настраиваем JSON-сериализацию
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }
}