package no.elg.infiniteBootleg.world.box2d

import ktx.collections.GdxArray
import ktx.collections.GdxLongArray

class LongContactTracker(private val userData: Any) {

  val filter: (userData: Any?) -> Boolean = { it == userData }
  private val contactFixtures = GdxLongArray(false, 16)

  val size: Int
    get() = contactFixtures.size
  val isEmpty: Boolean
    get() = contactFixtures.isEmpty
  val isNotEmpty: Boolean
    get() = !contactFixtures.isEmpty

  fun clear() = contactFixtures.clear()

  fun add(value: Long) {
    contactFixtures.add(value)
  }

  fun remove(value: Long) = contactFixtures.removeValue(value)

  fun removeAll(value: Long) {
    while (contactFixtures.removeValue(value)) {
      // Empty on purpose, removeValue has side effects
    }
  }
}

class ObjectContactTracker<T : Any> {

  private val contactFixtures = GdxArray<T>(false, 16)

  val size: Int
    get() = contactFixtures.size
  val isEmpty: Boolean
    get() = contactFixtures.isEmpty

  fun clear() = contactFixtures.clear()

  fun add(value: T) {
    contactFixtures.add(value)
  }

  fun remove(value: T) = contactFixtures.removeValue(value, true)

  fun removeAll(value: T) {
    while (contactFixtures.removeValue(value, true)) {
      // Empty on purpose, removeValue has side effects
    }
  }
}
