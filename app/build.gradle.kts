import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    alias(libs.plugins.shadow)
    alias(libs.plugins.ktor)
    id("dev.detekt")
    application
}

application {
    mainClass.set("no.nav.aap.arenaoppslag.AppKt")
}

detekt {
    ignoreFailures = true
    autoCorrect = true
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget.set("21")
}

dependencies {

    // Overstyr versjoner ktor setter, for å få sikkerhetsfikser
    implementation(platform(libs.netty.bom))
    implementation(platform(libs.jackson.bom))
    // Overstyr versjoner logstash setter, for å få sikkerhetsfikser
    implementation(platform(libs.jackson3.bom))

    implementation(project(":kontrakt"))
<<<<<<< HEAD
    implementation(libs.konfig)
    implementation(libs.kelvin.server)
    implementation(libs.kelvin.infrastructure)

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.logging.jvm)
    implementation(libs.ktor.server.call.id)

    implementation(libs.ktor.serialization.jackson)
    implementation(libs.jackson.datatype.jsr310)

<<<<<<< HEAD
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.logback.classic)
    runtimeOnly(libs.logstash.logback.encoder)

    implementation(libs.caffeine)

    implementation(libs.oracle.ojdbc11)
    implementation(libs.hikaricp)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.nimbus.jose.jwt)
    testImplementation(libs.flyway.core)
    testImplementation(libs.assertj.core)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.h2)
    testImplementation(libs.mockk)
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
