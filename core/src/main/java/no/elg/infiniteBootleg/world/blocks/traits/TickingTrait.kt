package no.elg.infiniteBootleg.world.blocks.traits

import no.elg.infiniteBootleg.Ticking

/**
 * @author Elg
 */
interface TickingTrait : Trait, Ticking {

  /**
   * Update if the update flag is set to true
   *
   * @param rare If the rare update should be called instead of the normal update
   */
  fun tryTick(rare: Boolean)

  fun shouldTick(): Boolean

  fun setShouldTick(boolean: Boolean)

  fun delayedShouldTick(delayTicks: Long)
}
