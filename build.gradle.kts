import buildsrc.ext.excludeGeneratedGradleDsl

plugins {
  buildsrc.convention.`kotlin-jvm`
  `kotlin-dsl`
  `java-gradle-plugin`
  `jvm-test-suite`

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


@Suppress("UnstableApiUsage") // jvm test suites are incubating
testing.suites {
  val test by getting(JvmTestSuite::class) {
    useJUnitJupiter()

    dependencies {
      implementation("io.kotest:kotest-assertions-core:5.5.4")
      implementation("io.kotest:kotest-runner-junit5:5.5.4")

      implementation("io.mockk:mockk:1.13.3")
    }
  }

  val functionalTest by registering(JvmTestSuite::class) {
    useJUnitJupiter()
    testType.set(TestSuiteType.FUNCTIONAL_TEST)

    dependencies {
      implementation("io.kotest:kotest-assertions-core:5.5.4")
      implementation("io.kotest:kotest-runner-junit5:5.5.4")

      implementation(project.dependencies.gradleTestKit())
    }

    targets.configureEach {
      testTask.configure {
        shouldRunAfter(test)
        dependsOn(tasks.matching { it.name == "publishAllPublicationsToProjectLocalRepository" })
        systemProperties(
          "projectLocalMavenRepo" to rootProject.layout.buildDirectory.dir("maven-project-local")
            .map { it.asFile.invariantSeparatorsPath }.get(),
        )
      }
    }

    sources {
      java {
        resources {
          srcDir(tasks.pluginUnderTestMetadata.flatMap { it.outputDirectory })
        }
      }
    }

    gradlePlugin.testSourceSet(sources)
  }

  tasks.check { dependsOn(functionalTest) }
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
