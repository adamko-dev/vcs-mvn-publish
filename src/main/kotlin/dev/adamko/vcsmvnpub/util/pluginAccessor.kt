@file:Suppress("PackageDirectoryMismatch", "ObjectPropertyName")

package dev.adamko

import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

/**
 * The Gradle plugin implemented by [dev.adamko.vcsmvnpub.VcsMvnPublishPlugin].
 *
 * @see dev.adamko.vcsmvnpub.VcsMvnPublishPlugin
 */
fun PluginDependenciesSpec.`vcs-mvn-publish`(): PluginDependencySpec =
  id("dev.adamko.vcs-mvn-publish")
