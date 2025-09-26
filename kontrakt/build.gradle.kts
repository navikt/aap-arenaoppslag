import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    `maven-publish`
    `java-library`
}

java {
    withSourcesJar()
}
kotlin {
    explicitApi = ExplicitApiMode.Warning
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
            version = project.findProperty("version")?.toString() ?: "0.0.0"
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/aap-arenaoppslag")
            credentials {
                username = "x-access-token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}