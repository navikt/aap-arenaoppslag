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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
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
