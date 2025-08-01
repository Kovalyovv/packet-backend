package ru.packet.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.koin.dsl.module
import org.jetbrains.exposed.sql.Database
import ru.packet.database.DatabaseConfig
import ru.packet.services.*
import kotlinx.serialization.json.Json

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
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }
}