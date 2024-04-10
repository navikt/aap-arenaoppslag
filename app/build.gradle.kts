import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.10"
    application
}

val ktorVersion = "2.3.10"

application {
    mainClass.set("arenaoppslag.AppKt")
}

dependencies {
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.5.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.4")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.4")

    implementation("com.oracle.database.jdbc:ojdbc11:23.3.0.23.09")
    implementation("com.zaxxer:HikariCP:5.1.0")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    testImplementation("org.flywaydb:flyway-core:10.11.0")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("com.h2database:h2:2.2.224")
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
    withType<Test> {
        useJUnitPlatform()
    }
}

kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")