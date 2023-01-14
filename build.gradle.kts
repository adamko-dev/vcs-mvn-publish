import buildsrc.ext.excludeGeneratedGradleDsl

plugins {
  buildsrc.convention.`kotlin-jvm`
  `kotlin-dsl`
  `java-gradle-plugin`

  id("me.qoomon.git-versioning")

  id("com.gradle.plugin-publish")
  buildsrc.convention.`maven-publish`

  idea

  dev.adamko.`vcs-mvn-publish`
}


group = "dev.adamko.vcsmvnpub"
version = "0.0.0-SNAPSHOT"
gitVersioning.apply {
  refs {
    branch(".+") { version = "\${ref}-SNAPSHOT" }
    tag("v(?<version>.*)") { version = "\${ref.version}" }
  }

  // optional fallback configuration in case of no matching ref configuration
  rev { version = "\${commit}" }
}


dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:$embeddedKotlinVersion"))
  implementation(libs.kotlin.utilIO)
}


gradlePlugin {
  plugins {
    register("VcsMvnPublish") {
      id = "dev.adamko.vcs-mvn-publish"
      implementationClass = "dev.adamko.vcsmvnpub.VcsMvnPublishPlugin"
    }
  }
}

vcsMvnPublish {
  gitRepo("artifacts")
}

idea {
  module {
    isDownloadSources = true
    isDownloadJavadoc = false
    excludeGeneratedGradleDsl(layout)
    excludeDirs = excludeDirs + layout.files(
      ".idea",
      "gradle/wrapper",
    )
  }
}
