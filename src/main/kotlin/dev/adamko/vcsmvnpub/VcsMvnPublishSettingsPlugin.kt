package dev.adamko.vcsmvnpub

import dev.adamko.vcsmvnpub.tasks.GitRepoInitTask
import dev.adamko.vcsmvnpub.tasks.GitRepoPublishTask
import java.net.URI
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType


abstract class VcsMvnPublishSettingsPlugin @Inject constructor(
  private val providers: ProviderFactory,
) : Plugin<Settings>, ProviderFactory by providers {


  private val log: Logger = Logging.getLogger(this::class.java)

  override fun apply(settings: Settings) {

    settings.

  }

}
