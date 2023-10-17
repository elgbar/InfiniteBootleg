package no.elg.infiniteBootleg.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import ktx.collections.GdxLongArray
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.decompactLocX
import no.elg.infiniteBootleg.util.decompactLocY
import no.elg.infiniteBootleg.util.isWithin
import no.elg.infiniteBootleg.util.relativeCompact
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.api.restriction.AuthoritativeSystem
import no.elg.infiniteBootleg.world.ecs.components.ChunkComponent.Companion.chunk
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.leafBlockFamily
import no.elg.infiniteBootleg.world.ecs.system.FamilyEntitySystem

object LeavesDecaySystem : FamilyEntitySystem(leafBlockFamily, UPDATE_PRIORITY_DEFAULT), AuthoritativeSystem {

  private const val DESPAWN_LEAVES_RADIUS = 5f

  override fun processEntities(entities: ImmutableArray<Entity>, deltaTime: Float) {
    val entity: Entity = entities.random()
    val chunk = entity.chunk
    if (chunk.isDisposed) {
      return
    }
    val srcLoc = entity.compactBlockLoc

    val stack = GdxLongArray(false, 16)
    val seen = GdxLongArray()

    stack.add(srcLoc)

    while (stack.notEmpty()) {
      val loc = stack.removeIndex(0)
      seen.add(loc)
      for (dir in Direction.CARDINAL) {
        val nextLoc = relativeCompact(loc.decompactLocX(), loc.decompactLocY(), dir)
        if (!isWithin(srcLoc, nextLoc, DESPAWN_LEAVES_RADIUS)) {
          // Block is too far away to be connected
          continue
        }
        val neighborMaterial = entity.world.getMaterial(nextLoc, false)

        if (neighborMaterial == Material.BIRCH_TRUNK) {
          // If there is a trunk connected to this leaf block, we don't want to despawn it and can return early
          return
        }
        if (neighborMaterial == Material.BIRCH_LEAVES && nextLoc !in seen) {
          stack.add(nextLoc)
        }
      }
    }

    // If no trunks were found, remove the leaf block
    entity.chunk.removeBlock(srcLoc.decompactLocX().chunkOffset(), srcLoc.decompactLocY().chunkOffset())
  }
}
