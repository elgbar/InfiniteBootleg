package no.elg.infiniteBootleg.util

import java.util.UUID

fun fromUUIDOrNull(string: String?): UUID? {
  return try {
    UUID.fromString(string)
  } catch (e: Exception) {
    null
  }
}
