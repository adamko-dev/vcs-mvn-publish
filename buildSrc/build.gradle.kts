import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
  kotlin("jvm") version embeddedKotlinVersion
}


dependencies {

  val kotlinVersion = libs.versions.kotlin.get()
//  val kotlinVersion = embeddedKotlinVersion
  implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
  implementation("org.jetbrains.kotlin:kotlin-serialization")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

  val gitVersioningPluginVersion = "5.2.0"
  implementation("me.qoomon:gradle-git-versioning-plugin:$gitVersioningPluginVersion")

  implementation("com.gradle.publish:plugin-publish-plugin:1.0.0-rc-2")
}


val gradleJvmTarget = "1.8"
val gradleJvmVersion = "8"


tasks.withType<KotlinCompile>().configureEach {

  kotlinOptions {
    jvmTarget = gradleJvmTarget
  }

  kotlinOptions.freeCompilerArgs += listOf(
    "-opt-in=kotlin.RequiresOptIn",
    "-opt-in=kotlin.ExperimentalStdlibApi",
    "-opt-in=kotlin.time.ExperimentalTime",
  )
}


kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(gradleJvmVersion))
  }

  kotlinDslPluginOptions {
    jvmTarget.set(gradleJvmTarget)
  }
}
