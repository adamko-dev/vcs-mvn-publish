@file:Suppress("PackageDirectoryMismatch")

package org.intellij.lang.annotations

import org.jetbrains.annotations.NonNls


@Retention(AnnotationRetention.SOURCE)
@Target(
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.FIELD,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.EXPRESSION
)
annotation class Language(
  val value: @NonNls String,
  val prefix: @NonNls String = "",
  val suffix: @NonNls String = ""
)
