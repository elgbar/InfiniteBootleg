package no.elg.infiniteBootleg.world.blocks

import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.blocks.traits.FallingTrait
import no.elg.infiniteBootleg.world.blocks.traits.FallingTraitHandler

/**
 * A blocks that falls when ticked
 */
class FallingBlock(
  world: World,
  chunk: Chunk,
  localX: Int,
  localY: Int,
  material: Material
) : TickingBlock(world, chunk, localX, localY, material), FallingTrait {

  override val falling: Boolean
    get() = fallingTraitHandler.falling

  private val fallingTraitHandler = FallingTraitHandler(
    world,
    CoordUtil.chunkToWorld(chunk.chunkX, localX),
    CoordUtil.chunkToWorld(chunk.chunkY, localY)
  )

  override fun tryFall() {
    fallingTraitHandler.tryFall()
  }

  override fun createEntityFromBlock() {
    fallingTraitHandler.createEntityFromBlock(this)
  }
}
