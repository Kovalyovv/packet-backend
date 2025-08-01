package ru.packet.models



import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.jodatime.timestampWithTimeZone
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import ru.packet.models.Groups.default


object Users: Table("users" ) {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 40)
    val email = varchar("email", 50).uniqueIndex()
    val passwordHash = text("password_hash")
    val role = text("role")

    val createdAt = datetime("created_at").default(DateTime.now(DateTimeZone.UTC))

    override val primaryKey = PrimaryKey(id, name = "PK_Users_ID")

}