package no.elg.infiniteBootleg.core.world.box2d

import it.unimi.dsi.fastutil.longs.LongArraySet
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.isBlockInsideRadius

class LongContactTracker(private val userData: Any) {

  val filter: (userData: Any?) -> Boolean = { it == userData }
  private val contactFixtures = LongArraySet()

  val size: Int
    get() = contactFixtures.size
  val isEmpty: Boolean
    get() = contactFixtures.isEmpty()
  val isNotEmpty: Boolean
    get() = !contactFixtures.isEmpty()

  fun clear() = contactFixtures.clear()

  fun add(value: WorldCompactLoc) {
    contactFixtures.add(value)
  }

  fun remove(value: WorldCompactLoc) = contactFixtures.remove(value)

  operator fun contains(value: Long) = contactFixtures.contains(value)

  fun validate(entityPos: WorldCompactLoc, cutoffRadius: Double) {
    val (x2: WorldCoord, y2: WorldCoord) = entityPos
    contactFixtures.removeIf { contactPos ->
      val (x1: WorldCoord, y1: WorldCoord) = contactPos
      !isBlockInsideRadius(x1, y1, x2, y2, cutoffRadius)
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
}
