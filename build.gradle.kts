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
    implementation("io.ktor:ktor-server-status-pages:2.3.8")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-auth:2.3.8")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.8")
    implementation("io.ktor:ktor-server-core:2.3.8")
    implementation("io.ktor:ktor-server-netty:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.jsonwebtoken:jjwt:0.12.6")
    implementation("io.ktor:ktor-server-config-yaml")
    testImplementation("io.ktor:ktor-server-test-host")

    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("io.insert-koin:koin-core:3.5.6")
    implementation ("org.mindrot:jbcrypt:0.4")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("org.jetbrains.exposed:exposed-core:0.48.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.48.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.48.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.44.1")
    implementation("org.postgresql:postgresql:42.7.2")



}
