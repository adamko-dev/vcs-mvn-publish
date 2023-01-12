import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
}


dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:$embeddedKotlinVersion"))
  implementation("org.jetbrains.kotlin:kotlin-serialization")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")

  val gitVersioningPluginVersion = "5.2.0"
  implementation("me.qoomon:gradle-git-versioning-plugin:$gitVersioningPluginVersion")

  implementation("com.gradle.publish:plugin-publish-plugin:1.0.0")
}


val gradleJvmTarget = "11"

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(gradleJvmTarget))
  }
}

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
