package dev.adamko.vcsmvnpub.util

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider


internal fun <T> Property<T>.set(value: T?, convention: T?) {
  set(value)
  if (convention != null) {
    convention(convention)
  }
}


internal fun <T> Property<T>.set(value: T?, convention: Provider<T?>?) {
  set(value)
  if (convention != null) {
    convention(convention)
  }
}


internal fun <T> Property<T>.set(value: Provider<T?>, convention: Provider<T?>?) {
  set(value)
  if (convention != null) {
    convention(convention)
  }
}


internal fun <T> Property<T>.set(value: Provider<T?>, convention: T?) {
  set(value)
  convention(convention)
}
