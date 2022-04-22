package no.elg.infiniteBootleg.world.blocks

import no.elg.infiniteBootleg.world.BlockImpl
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.blocks.traits.TickingTrait
import no.elg.infiniteBootleg.world.blocks.traits.TickingTraitHandler

/**
 * Describes a block that implements the [Ticking] interface.
 *
 *
 * This implementation is set up to automatically call [.tick] and [.tickRare] if
 * [.shouldTick] is `true`. To change this behaviour override [.shouldTick]
 *
 *
 * For each chunk has a list of blocks that extends this class and will call [ ][.tryTick] every tick.
 *
 * @see Chunk
 *
 * @see TntBlock TntBlock
 */
abstract class TickingBlock protected constructor(
  world: World,
  chunk: Chunk?,
  localX: Int,
  localY: Int,
  material: Material
) : BlockImpl(world, chunk!!, localX, localY, material), TickingTrait {

  private val tickingTrait: TickingTraitHandler

  init {
    tickingTrait = TickingTraitHandler(this, world.worldTicker)
  }

  override fun tryTick(rare: Boolean) {
    tickingTrait.tryTick(rare)
  }

  override fun shouldTick(): Boolean = tickingTrait.shouldTick

  override fun enableTick() {
    tickingTrait.shouldTick = true
  }

  override fun delayedShouldTick(delayTicks: Long) {
    tickingTrait.delayedShouldTick(delayTicks)
  }
}
