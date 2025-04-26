package ru.packet.di

import org.koin.dsl.module
import org.jetbrains.exposed.sql.Database
import ru.packet.database.DatabaseConfig
import ru.packet.services.*

val appModule = module {
    single<Database> { DatabaseConfig.init() }
    single { UserService(get()) }
    single { GroupService(get()) }
    single { ActivityService(get()) }
    single { ItemService(get()) }
    single { ListService(get()) }
    single { PersonalListService()}
    single { ReceiptService(get()) }
    single { ChatService(get()) }
}