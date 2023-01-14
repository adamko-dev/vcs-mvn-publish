package dev.adamko.vcsmvnpub.util

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language


// utils for testing using Gradle TestKit


open class GradleProjectTest(
  private val projectDir: Path = createTempDirectory("vcsmvnpub")
) {
  val runner: GradleRunner = GradleRunner.create().withProjectDir(projectDir.toFile())

  val projectFiles: MutableMap<String, String> = mutableMapOf<String, String>().withDefault { "" }

  @Language("properties")
  var gradleProperties: String by projectFile("gradle.properties")

  init {
    gradleProperties = """
        kotlin.mpp.stability.nowarn=true
        org.gradle.cache=true
     """.trimIndent()
  }

  fun createFile(filename: String, contents: String): Path =
    projectDir.resolve(filename).apply {
      parent.createDirectories()
      writeText(contents)
    }

  fun projectFile(
    @Language("file-reference") filename: String,
  ): ReadWriteProperty<GradleProjectTest, String> {
    projectFiles[filename] = ""
    return object : ReadWriteProperty<GradleProjectTest, String> {
      override fun getValue(thisRef: GradleProjectTest, property: KProperty<*>): String =
        projectFiles.getValue(filename)

      override fun setValue(thisRef: GradleProjectTest, property: KProperty<*>, value: String) =
        projectFiles.set(filename, value)
    }
  }
}


/** Builder for testing a Gradle project that uses Kotlin script DSL */
class GradleKtsProjectTest(
  projectDir: Path
) : GradleProjectTest(projectDir) {

  @Language("kts")
  var settingsGradleKts: String by projectFile("settings.gradle.kts")

  @Language("kts")
  var buildGradleKts: String by projectFile("build.gradle.kts")

  init {
    settingsGradleKts = """
      rootProject.name = "vcs-mvn-publish-test"
    """.trimIndent()
  }

  companion object {
    fun gradleKtsProjectTest(
      projectDir: Path = createTempDirectory("vcsmvnpub-kts"),
      build: GradleKtsProjectTest.() -> Unit,
    ): GradleKtsProjectTest {
      val gradleTest = GradleKtsProjectTest(projectDir).apply(build)

      projectDir.apply {
        gradleTest.createFile("build.gradle.kts", gradleTest.buildGradleKts)
        gradleTest.createFile("settings.gradle.kts", gradleTest.settingsGradleKts)
        gradleTest.createFile("gradle.properties", gradleTest.gradleProperties)
      }

      return gradleTest
    }
  }
}


/** Builder for testing a Gradle project that uses Groovy script*/
class GradleGroovyProjectTest(
  projectDir: Path
) : GradleProjectTest(projectDir) {

  @Language("groovy")
  var settingsGradle: String by projectFile("settings.gradle")

  @Language("groovy")
  var buildGradle: String by projectFile("build.gradle")

  init {
    settingsGradle = """
      rootProject.name = "vcs-mvn-publish-test"
    """.trimIndent()
  }

  companion object {
    fun gradleGroovyProjectTest(
      projectDir: Path = createTempDirectory("vcsmvnpub-groovy"),
      build: GradleGroovyProjectTest.() -> Unit,
    ): GradleGroovyProjectTest {
      val gradleTest = GradleGroovyProjectTest(projectDir).apply(build)

      projectDir.apply {
        gradleTest.createFile("build.gradle", gradleTest.buildGradle)
        gradleTest.createFile("settings.gradle", gradleTest.settingsGradle)
        gradleTest.createFile("gradle.properties", gradleTest.gradleProperties)
      }

      return gradleTest
    }
  }
}
