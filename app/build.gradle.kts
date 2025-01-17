plugins {
    id("io.ktor.plugin") version "3.0.3"
    application
}

val ktorVersion = "3.0.3"

application {
    mainClass.set("arenaoppslag.AppKt")
}

dependencies {
    implementation(project(":kontrakt"))

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

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.3")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:8.0")

    implementation("com.oracle.database.jdbc:ojdbc11:23.6.0.24.10")
    implementation("com.zaxxer:HikariCP:6.2.1")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:10.0.1")
    testImplementation("org.flywaydb:flyway-core:11.2.0")
    testImplementation("org.assertj:assertj-core:3.27.2")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("com.h2database:h2:2.3.232")
}