package buildsrc.convention

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("buildsrc.convention.base")
  kotlin("jvm")
  `java-library`
}

val projectJvmTarget = "11"
val projectJvmVersion = "11"

//kotlin {
//  jvmToolchain {
//    languageVersion.set(JavaLanguageVersion.of(projectJvmVersion))
//  }
//}

tasks.withType<KotlinCompile>().configureEach {

  kotlinOptions {
    jvmTarget = projectJvmTarget
  }

  kotlinOptions.freeCompilerArgs += listOf(
    "-opt-in=kotlin.RequiresOptIn",
    "-opt-in=kotlin.ExperimentalStdlibApi",
    "-opt-in=kotlin.time.ExperimentalTime",
//    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
//    "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
  )
}

//tasks.compileTestKotlin {
//  kotlinOptions.freeCompilerArgs += "-opt-in=io.kotest.common.ExperimentalKotest"
//}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

//java {
//  withJavadocJar()
//  withSourcesJar()
//}
