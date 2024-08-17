package no.elg.infiniteBootleg.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.utils.LongMap
import ktx.collections.plusAssign
import ktx.collections.removeAll
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.api.restriction.system.UniversalSystem
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.OccupyingBlocksComponent.Companion.occupyingLocations
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.standaloneGridOccupyingBlocksFamily

/**
 * Sets a marker block in the world to indicate that an entity is occupying that block
 */
object UpdateGridBlockSystem :
  IteratingSystem(standaloneGridOccupyingBlocksFamily, UPDATE_PRIORITY_DEFAULT),
  UniversalSystem {

  val chunkCache = LongMap<Chunk>()

  override fun update(deltaTime: Float) {
    chunkCache.clear()
    super.update(deltaTime)
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val pos = entity.positionComponent
    val halfBox2dWidth = entity.box2d.halfBox2dWidth
    val halfBox2dHeight = entity.box2d.halfBox2dHeight

    //Note: raw must be false to properly update the lights while lights are falling
    val currentOccupations =
      world.getBlocksAABB(pos.blockX.toFloat(), pos.blockY.toFloat(), halfBox2dWidth, halfBox2dHeight, raw = false, loadChunk = true, includeAir = true, chunkCache)
    val occupyingLocations = entity.occupyingLocations
    occupyingLocations.filter { it !in currentOccupations }.forEach {
      it.removeEntityMarker(true, chunkCache[it.chunk.compactLocation])
    }
    occupyingLocations.removeAll { it !in currentOccupations }

    for (currentOccupation in currentOccupations) {
      if ((currentOccupation !is EntityMarkerBlock || currentOccupation.entity != entity) && currentOccupation.material == Material.AIR) {
        occupyingLocations += EntityMarkerBlock.replaceBlock(currentOccupation, entity)
      }
    }
  }
}
