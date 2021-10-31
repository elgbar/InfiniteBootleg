package no.elg.infiniteBootleg.util

import com.fasterxml.uuid.Generators
import java.util.UUID

fun fromUUIDOrNull(string: String?): UUID? {
  return try {
    UUID.fromString(string)
  } catch (e: Exception) {
    null
  }
}

private val namespace = UUID.fromString("1aeeb167-a72e-45d3-8b75-8a144e56ca54")
private val uuidv5Generator = Generators.nameBasedGenerator(namespace)

fun randomUUIDFromString(string: String): UUID {
  return uuidv5Generator.generate(string)
}
