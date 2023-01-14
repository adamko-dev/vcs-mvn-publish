package dev.adamko.vcsmvnpub

import dev.adamko.vcsmvnpub.util.GradleKtsProjectTest.Companion.gradleKtsProjectTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain

class KtsFunctionalTest : FunSpec({
  context("test kts project") {

    val projectLocalMavenRepo: String = System.getProperty("projectLocalMavenRepo")

    val testProject = gradleKtsProjectTest {

      settingsGradleKts += """
        |pluginManagement {
        |  repositories {
        |    maven(url = "$projectLocalMavenRepo")
        |    gradlePluginPortal()
        |  }
        |}
      """.trimMargin()

      buildGradleKts = """
        |plugins {
        |  `embedded-kotlin`
        |  `maven-publish`
        |  id("dev.adamko.vcs-mvn-publish") version "main-SNAPSHOT"
        |}
        |
        |publishing {
        |  publications {
        |    create<MavenPublication>("maven") {
        |      groupId = "org.gradle.sample"
        |      artifactId = "library"
        |      version = "1.1"
        |
        |      from(components["java"])
        |    }
        |  }
        |}
        |
        |vcsMvnPublish {
        |  gitRepo("artifacts")
        |}
        |
      """.trimMargin()

      createFile(
        "src/main/java/main.kt", /* language=kotlin */ """
        |fun main() {
        |  println("Hello, world!")
        |}
        |
      """.trimMargin()
      )
    }

    test("expect vcs-mvn-publish tasks can be listed") {
      testProject.runner
        .forwardOutput()
        .withArguments("tasks")
        .build()
        .output.should { output ->
          output shouldContain "Vcs mvn publish tasks"
          output shouldContain "gitRepoInit"
          output shouldContain "Run all GitRepoInit tasks"
          output shouldContain "gitRepoInitArtifacts"
          output shouldContain "gitRepoPublish"
          output shouldContain "Run all GitRepoPublish tasks"
          output shouldContain "gitRepoPublishArtifacts"
          output shouldContain "commit and push files to a Git repo after a Maven publication"
        }
    }
  }
})
