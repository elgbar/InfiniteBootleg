package no.elg.infiniteBootleg.world.blocks

import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.traits.FallingTrait
import no.elg.infiniteBootleg.world.blocks.traits.FallingTraitHandler
import no.elg.infiniteBootleg.world.world.World

/**
 * A block that lights up the surrounding area
 */
class Torch(world: World, chunk: Chunk, localX: Int, localY: Int, material: Material) : TickingBlock(world, chunk, localX, localY, material), FallingTrait {

  private val fallingTraitHandler = FallingTraitHandler(
    this,
    world,
    chunkToWorld(chunk.chunkX, localX),
    chunkToWorld(chunk.chunkY, localY),
    Material.TORCH
  )

  override val falling: Boolean get() = fallingTraitHandler.falling

  override fun tryFall() {
    fallingTraitHandler.tryFall()
  }
}
