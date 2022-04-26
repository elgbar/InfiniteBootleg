package no.elg.infiniteBootleg.world.blocks.traits

import no.elg.infiniteBootleg.util.Ticker
import java.util.concurrent.ThreadLocalRandom

/**
 * @author Elg
 */
class TickingTraitHandler(private val tickingTrait: TickingTrait, private val worldTicker: Ticker) : TraitHandler<TickingTrait> {

  @Volatile
  private var minimumTick: Long = 0

  @field:Volatile
  var shouldTick = false
    set(value) {
      synchronized(tickLock) {
        if (this.shouldTick != value) {
          field = value
          minimumTick = worldTicker.tickId
        }
      }
    }

  private val tickLock = Any()

  init {
    tickingTrait.handlers.set<TickingTrait>(this)
    synchronized(tickLock) {
      shouldTick = false
      // spread ticking over multiple ticks to ease strain on ticker
      minimumTick = (worldTicker.tickId + ThreadLocalRandom.current().nextInt(worldTicker.tps.toInt() / 3))
    }
  }

  /**
   * Update if the update flag is set to true
   *
   * @param rare If the the rare update should be called instead of the normal update
   */
  fun tryTick(rare: Boolean) {
    synchronized(tickLock) {
      // should not tick right away to not spawn multiple entities when spawning e.g., sand
      if (!tickingTrait.shouldTick() || minimumTick > worldTicker.tickId) {
        return
      }
      shouldTick = false
    }
    if (rare) {
      tickingTrait.tickRare()
    } else {
      tickingTrait.tick()
    }
  }

  fun delayedShouldTick(delayTicks: Long) {
    synchronized(tickLock) {
      shouldTick = true
      minimumTick += delayTicks
    }
  }
}
