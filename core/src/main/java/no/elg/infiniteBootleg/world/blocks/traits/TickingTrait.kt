package no.elg.infiniteBootleg.world.blocks.traits

import no.elg.infiniteBootleg.Ticking

/**
 * @author Elg
 */
interface TickingTrait : Trait, Ticking {

  /**
   * Call [tick] or [tickRare] if [shouldTick] is `true`
   *
   * @param rare If the rare update should be called instead of the normal update
   */
  fun tryTick(rare: Boolean)

  /**
   * If ticking should be done
   */
  fun shouldTick(): Boolean

  /**
   * Say we should tick
   */
  fun enableTick()

  /**
   * Enable ticking after [delayTicks] ticks
   */
  fun delayedShouldTick(delayTicks: Long)
}
