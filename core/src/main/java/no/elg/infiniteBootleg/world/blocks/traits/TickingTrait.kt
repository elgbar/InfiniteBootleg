package no.elg.infiniteBootleg.world.blocks.traits

import no.elg.infiniteBootleg.Ticking

/**
 * @author Elg
 */
interface TickingTrait : BlockTrait, Ticking {

  fun shouldTick(): Boolean

  fun setShouldTick(boolean: Boolean)

  fun delayedShouldTick(delayTicks: Long)

}
