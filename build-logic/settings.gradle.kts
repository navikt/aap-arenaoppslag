rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    // Del versjonskatalogen fra hovedbygget, slik at build-logic kan bruke de samme versjonene
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
