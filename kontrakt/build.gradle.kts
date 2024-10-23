import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    `maven-publish`
    `java-library`
}

apply(plugin = "maven-publish")
apply(plugin = "java-library")

java {
    withSourcesJar()
}

kotlin {
    explicitApi = ExplicitApiMode.Warning
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
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