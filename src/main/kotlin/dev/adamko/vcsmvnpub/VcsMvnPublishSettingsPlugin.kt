package dev.adamko.vcsmvnpub

import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ProviderFactory


abstract class VcsMvnPublishSettingsPlugin @Inject constructor(
  private val providers: ProviderFactory,
) : Plugin<Settings>, ProviderFactory by providers {


  private val log: Logger = Logging.getLogger(this::class.java)

  override fun apply(target: Settings) {

  }

}
