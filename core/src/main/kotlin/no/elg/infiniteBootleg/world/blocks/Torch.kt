package no.elg.infiniteBootleg.world.blocks

import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.blocks.traits.FallingTrait
import no.elg.infiniteBootleg.world.blocks.traits.FallingTraitHandler

/**
 * A block that lights up the surrounding area
 */
class Torch(world: World, chunk: Chunk, localX: Int, localY: Int, material: Material) : TickingBlock(world, chunk, localX, localY, material), FallingTrait {

  private val fallingTraitHandler = FallingTraitHandler(
    this,
    world,
    CoordUtil.chunkToWorld(chunk.chunkX, localX),
    CoordUtil.chunkToWorld(chunk.chunkY, localY),
    Material.TORCH
  )

  override val falling: Boolean get() = fallingTraitHandler.falling

  override fun tryFall() {
    fallingTraitHandler.tryFall()
  }
}
