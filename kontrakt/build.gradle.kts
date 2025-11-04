import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
    `maven-publish`
    `java-library`
}

java {
    withSourcesJar()
}

// Håndteres eksplisitt for å sette duplicateStrategy, som ellers feiler
(tasks.findByName("sourcesJar") as? Jar)?.apply {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
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