package no.elg.infiniteBootleg.util

import no.elg.infiniteBootleg.api.Metadata
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread safe configuration
 *
 * @author Elg
 */
class Config : Metadata {
  private val config: MutableMap<String, Any?> = ConcurrentHashMap()
  override fun <T> get(key: String, defaultVal: T?): T? {
    val obj = config.getOrDefault(key, defaultVal)
    return try {
      @Suppress("UNCHECKED_CAST")
      obj as? T?
    } catch (e: ClassCastException) {
      throw IllegalStateException("Failed to cast object to type T", e)
    }
  }

  override fun <T> set(key: String, value: T) {
    config[key] = value
  }
}
