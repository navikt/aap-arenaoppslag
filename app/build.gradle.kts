import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    id("aap.conventions")
    id("com.gradleup.shadow") version "9.3.0"
    id("io.ktor.plugin") version "3.3.3"
    application
}

val ktorVersion = "3.3.3"

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
    implementation("no.nav.aap.kelvin:json:1.0.475")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.1")
    implementation("ch.qos.logback:logback-classic:1.5.23")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:9.0")

    implementation("com.oracle.database.jdbc:ojdbc11:23.26.0.0.0")
    implementation("com.zaxxer:HikariCP:7.0.2")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:10.6")
    testImplementation("org.flywaydb:flyway-core:11.20.0")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("com.h2database:h2:2.4.240")
    testImplementation("io.mockk:mockk:1.14.7")
}

tasks {

    withType<ShadowJar> {
        // Duplikate class og ressurs-filer kan skape runtime-feil, fordi JVM-en velger den første på classpath
        // ved duplikater, og det kan være noe annet enn vår kode (og libs vi bruker) forventer.
        // Derfor logger vi en advarsel hvis vi oppdager duplikater.
        duplicatesStrategy = DuplicatesStrategy.WARN

        mergeServiceFiles()

        filesMatching(listOf("META-INF/io.netty.*", "META-INF/services/**", "META-INF/maven/**")) {
            // For disse filene fra upstream, antar vi at de er identiske hvis de har samme navn.
            // Merk at META-INF/maven/org.webjars/swagger-ui/pom.properties
            // brukes av com.papsign.ktor.openapigen.SwaggerUIVersion
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            // Vi beholder alle pom.properties fra Maven for å støtte generering av SBOM i Nais
        }

        // Helt unødvendige filer som ofte skaper duplikater
        val fjernDisseDuplikatene = listOf(
            "*.SF", "*.DSA", "*.RSA", // Signatur-filer som ikke trengs på runtime
            "*NOTICE*", "*LICENSE*", "*DEPENDENCIES*", "*README*", "*COPYRIGHT*", // til mennesker bare
            "proguard/**", // Proguard-konfigurasjoner som ikke trengs på runtime
            "com.android.tools/**" // Android build-filer som ikke trengs på runtime
        )
        fjernDisseDuplikatene.forEach { pattern -> exclude("META-INF/$pattern") }
    }
}
