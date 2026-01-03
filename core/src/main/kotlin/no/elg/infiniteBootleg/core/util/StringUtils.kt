package no.elg.infiniteBootleg.core.util

val CAMELCASE_REGEX = "([A-Z][a-z]|[A-Z]+(?![a-z]))".toRegex()

fun String.toTitleCase(): String = (lowercase() as CharSequence).toTitleCase()
fun CharSequence.toTitleCase(): String {
  val title = CAMELCASE_REGEX.replace(this) { " ${it.value}" }
  val first = title.first()
  return "${if (first.isWhitespace()) "" else first.uppercaseChar()}${title.drop(1)}"
}

fun Boolean.toAbled(): String = if (this) "enabled" else "disabled"
