rootProject.name = "buildSrc"

apply(from = "./repositories.settings.gradle.kts")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }

  repositories {
    maven("https://raw.githubusercontent.com/adamko-dev/vcs-mvn-publish/artifacts/m2")
    maven(file("../build/maven-project-local"))
  }
}
