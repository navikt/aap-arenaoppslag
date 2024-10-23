plugins {
    id("io.ktor.plugin") version "3.0.0"
    application
}

val ktorVersion = "3.0.0"

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
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.5")
    implementation("ch.qos.logback:logback-classic:1.5.8")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:8.0")

    implementation("com.oracle.database.jdbc:ojdbc11:23.5.0.24.07")
    implementation("com.zaxxer:HikariCP:6.0.0")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:9.41.2")
    testImplementation("org.flywaydb:flyway-core:10.19.0")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("com.h2database:h2:2.3.232")
}