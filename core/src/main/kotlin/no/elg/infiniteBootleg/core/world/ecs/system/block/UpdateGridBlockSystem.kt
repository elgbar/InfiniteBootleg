package no.elg.infiniteBootleg.core.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.ashley.has
import ktx.collections.isNotEmpty
import ktx.collections.plusAssign
import ktx.collections.removeAll
import no.elg.infiniteBootleg.core.util.stringifyCompactLocWithChunk
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.validChunkOrLoad
import no.elg.infiniteBootleg.core.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent.Companion.occupyingBlocksComponent
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.standaloneGridOccupyingBlocksFamily

private val logger = KotlinLogging.logger {}

/**
 * Sets a marker block in the world to indicate that an entity is occupying that block
 */
object UpdateGridBlockSystem : IteratingSystem(standaloneGridOccupyingBlocksFamily, UPDATE_PRIORITY_DEFAULT) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val pos = entity.positionComponent
    val box2d = entity.box2d
    val occupyingBlocksComponent = entity.occupyingBlocksComponent

    if (!entity.has(VelocityComponent.mapper) && occupyingBlocksComponent.occupying.isNotEmpty()) {
      // The entity will not move, so only update it only once
      return
    }

    // Note: raw must be false to properly update the lights while lights are falling
    // Note 2: loadChunk must be false as the entity should then be handled as out of bounds
    val currentOccupations =
      world.getBlocksAABB(pos.blockX.toFloat(), pos.blockY.toFloat(), box2d.box2dWidth - 1f, box2d.box2dHeight - 1f, raw = false, loadChunk = false, includeAir = true)

    // Remove markers that are no longer occupied
    val noLongerOccupied = occupyingBlocksComponent.occupying.filter { it !in currentOccupations }
    occupyingBlocksComponent.occupying.removeAll(noLongerOccupied)

    val newOccupations = currentOccupations.filter { it !in occupyingBlocksComponent.occupying }
    for (newOccupation in newOccupations) {
      val validChunk = newOccupation.validChunkOrLoad ?: run {
        logger.error { "Failed to get valid chunk for block ${stringifyCompactLocWithChunk(newOccupation)}" }
        continue
      }
      if ((newOccupation !is EntityMarkerBlock || newOccupation.entity != entity) && newOccupation.material == Material.Air) {
        val occupiedBlock =
          EntityMarkerBlock.replaceBlock(validChunk, newOccupation.localX, newOccupation.localY, entity, occupyingBlocksComponent.hardLink, sendUpdatePacket = false) ?: run {
            logger.error { "Failed to replace marker block ${stringifyCompactLocWithChunk(newOccupation)}" }
            continue
          }
        occupyingBlocksComponent.occupying += occupiedBlock
      }
    }
    // Remove markers after setting new markers to avoid a gap where there is no marker at all
    noLongerOccupied.forEach(EntityMarkerBlock::removeEntityMarker)
  }
}
