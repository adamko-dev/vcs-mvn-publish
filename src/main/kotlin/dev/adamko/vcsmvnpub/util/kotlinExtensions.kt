package dev.adamko.vcsmvnpub.util


internal fun String.uppercaseFirstChar() = replaceFirstChar {
  when {
    !it.isUpperCase() -> it.titlecaseChar()
    else              -> it
  }
}

/** Replace characters that satisfy [matcher] with [new] */
private fun String.replace(new: Char, matcher: (Char) -> Boolean): String =
  map { c -> if (matcher(c)) new else c }.joinToString("")
