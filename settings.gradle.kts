pluginManagement {
    includeBuild("build-logic")
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "arenaoppslag"
include("app", "kontrakt")

dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") {
            // Speiler kun no.nav-artefakter, så vi slipper å slå opp tredjeparts-avhengigheter her
            content { includeGroupByRegex("no\\.nav.*") }
        }
        mavenCentral()
        maven("https://packages.confluent.io/maven/") {
            // Kun Confluent/Avro-avhengigheter hentes herfra
            content {
                includeGroupByRegex("io\\.confluent.*")
                includeGroup("org.apache.avro")
            }
        }
        mavenLocal()
    }
}
