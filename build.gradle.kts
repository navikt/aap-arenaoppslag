import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    kotlin("jvm") version "2.2.20"
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

subprojects {
    group = "no.nav.aap.arenaoppslag"
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks {
        withType<Test> {
            reports.html.required.set(false)
            useJUnitPlatform()
            maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
        }

        (findByName("distTar") as? Tar)?.apply {
            // Bruk et unikt navn for jar-filen til distTar, for å unngå navnekollisjoner i multi-modul prosjekt,
            // slik at vi ikke bruker samme navn, feks. "kontrakt.jar" "api.jar" i flere moduler.
            // Dette unngår feil av typen "Entry <name>.jar is a duplicate but no duplicate handling strategy has been set"
            archiveBaseName.set("${rootProject.name}-${project.name}")
        }

        kotlin.sourceSets["main"].kotlin.srcDirs("main/kotlin")
        kotlin.sourceSets["test"].kotlin.srcDirs("test/kotlin")
        sourceSets["main"].resources.srcDirs("main/resources")
        sourceSets["test"].resources.srcDirs("test/resources")

        kotlin {
            jvmToolchain(21)
            explicitApi = ExplicitApiMode.Warning
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
                languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
            }

        }

    }

    // Pass på at når vi kaller JavaExec eller Test tasks så bruker vi samme JVM som vi kompilerer med
    val toolchainLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    tasks.withType<Test>().configureEach { javaLauncher.set(toolchainLauncher) }
    tasks.withType<JavaExec>().configureEach { javaLauncher.set(toolchainLauncher) }

}
