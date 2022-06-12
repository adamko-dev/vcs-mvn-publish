import buildsrc.ext.excludeGeneratedGradleDsl

plugins {
  buildsrc.convention.`kotlin-jvm`
  `kotlin-dsl`
  `java-gradle-plugin`




  id("me.qoomon.git-versioning")

  id("com.gradle.plugin-publish")
  buildsrc.convention.`maven-publish`

//  `project-report`
//  `build-dashboard`
  idea
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
  implementation("org.jetbrains.kotlin:kotlin-util-io")
}


gradlePlugin {
  plugins {
    register("VcsMvnPublish") {
      id = "dev.adamko.vcs-mvn-publish"
      implementationClass = "dev.adamko.vcsmvnpub.VcsMvnPublishPlugin"
    }
  }
}


tasks.wrapper {
  gradleVersion = "7.5-rc-2"
  distributionType = Wrapper.DistributionType.ALL
}


idea {
  module {
    isDownloadSources = true
    isDownloadJavadoc = true
    excludeGeneratedGradleDsl(layout)
    excludeDirs = excludeDirs + layout.files(
      ".idea",
      "gradle/kotlin-js-store",
      "gradle/wrapper",
    )
  }
}

/**
 * The version of the Kotlin compiler embedded in gradle-kotlin-dsl (currently _${embeddedKotlinVersion}_).
 */
val embeddedKotlinVersion = "$123"
