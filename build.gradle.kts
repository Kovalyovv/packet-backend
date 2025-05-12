val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.1.20"
    id("io.ktor.plugin") version "3.1.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
}

group = "ru.packet"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor client dependencies (приводим версии к 2.3.8 для согласованности с серверными зависимостями)
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-cio:2.3.8")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.8") // Добавляем эту зависимость
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")

    // Ktor server dependencies
    implementation("io.ktor:ktor-server-status-pages:2.3.8")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.8")
    implementation("io.ktor:ktor-server-websockets:2.3.8")
    implementation("io.ktor:ktor-server-auth:2.3.8")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.8")
    implementation("io.ktor:ktor-server-core:2.3.8")
    implementation("io.ktor:ktor-server-netty:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
    implementation("io.ktor:ktor-server-config-yaml:2.3.8")
    testImplementation("io.ktor:ktor-server-test-host:2.3.8")

    // JWT
    implementation("io.jsonwebtoken:jjwt:0.12.6")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Database
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.jetbrains.exposed:exposed-core:0.48.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.48.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.48.0")
    implementation("org.jetbrains.exposed:exposed-jodatime:0.48.0")
    implementation("joda-time:joda-time:2.12.5")
    implementation("org.postgresql:postgresql:42.7.2")

    // Koin
    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("io.insert-koin:koin-core:3.5.6")

    // Other
    implementation("org.mindrot:jbcrypt:0.4")

    // Test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}