plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "arenaoppslag"
include("app", "kontrakt")


val githubPassword: String? by settings

dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/behandlingsflyt")
            credentials {
                username = "x-access-token"
                password = (githubPassword
                    ?: System.getenv("GITHUB_PASSWORD")
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: "").apply {
                    if (this.isBlank()) {
                        // Log as error instead of failing the build.
                        // This works around the GHA Automatic Dependency Submission (Gradle) validate-project step not passing on ENV values
                        logger.error("Either GITHUB_TOKEN or GITHUB_PASSWORD must be set in env to download NAV packages")
                    }
                }
            }
        }
        mavenLocal()
    }
}
