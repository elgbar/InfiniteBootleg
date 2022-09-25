package no.elg.infiniteBootleg.api

/**
 * @author Elg
 */
interface Metadata {
  operator fun <T> get(key: String): T? {
    return get<T?>(key, null)
  }

  /**
   * @param key Key to the castle
   * @param defaultVal Value to return if no object with the given key exists
   * @param <T> The type of the value
   * @return The value stored at `key`
   * @throws IllegalStateException If the object at `key` cannot be cast to `T`
   </T> */
  operator fun <T> get(key: String, defaultVal: T?): T?
  operator fun <T> set(key: String, value: T)
}
