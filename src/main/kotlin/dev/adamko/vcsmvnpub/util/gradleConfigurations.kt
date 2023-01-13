package dev.adamko.vcsmvnpub.util

import org.gradle.api.artifacts.Configuration


/** Mark this [Configuration] as one that will be consumed by other subprojects. */
internal fun Configuration.asProvider(visible: Boolean = false) {
  isVisible = visible
  isCanBeResolved = false
  isCanBeConsumed = true
}

/** Mark this [Configuration] as one that will consume (also known as 'resolving') artifacts from other subprojects */
internal fun Configuration.asConsumer(visible: Boolean = false) {
  isVisible = visible
  isCanBeResolved = true
  isCanBeConsumed = false
}
