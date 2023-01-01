package no.elg.infiniteBootleg.world.blocks

import no.elg.infiniteBootleg.world.BlockImpl
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.subgrid.MaterialEntity
import no.elg.infiniteBootleg.world.subgrid.Removable

/**
 * @author Elg
 */
class EntityBlock(
  world: World,
  chunk: Chunk,
  localX: Int,
  localY: Int,
  material: Material,
  val entity: MaterialEntity
) : BlockImpl(world, chunk, localX, localY, material), Removable {

  override fun hudDebug(): String {
    return "Entity type " + entity.simpleName() + " id " + entity.uuid
  }

  override fun onRemove() {
  }
}
