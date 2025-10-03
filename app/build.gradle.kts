plugins {
    id("io.ktor.plugin") version "3.3.0"
    application
}

val ktorVersion = "3.3.0"

application {
    mainClass.set("arenaoppslag.AppKt")
}

dependencies {
    implementation(project(":kontrakt"))
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("no.nav.aap.kelvin:json:1.0.382")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.4")
    implementation("ch.qos.logback:logback-classic:1.5.19")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:8.1")

    implementation("com.oracle.database.jdbc:ojdbc11:23.9.0.25.07")
    implementation("com.zaxxer:HikariCP:7.0.2")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:10.5")
    testImplementation("org.flywaydb:flyway-core:11.13.2")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("com.h2database:h2:2.4.240")
}