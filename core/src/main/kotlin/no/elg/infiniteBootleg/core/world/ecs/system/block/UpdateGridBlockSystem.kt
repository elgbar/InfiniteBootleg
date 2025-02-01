package no.elg.infiniteBootleg.core.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.collections.plusAssign
import ktx.collections.removeAll
import no.elg.infiniteBootleg.core.util.stringifyCompactLocWithChunk
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.validChunkOrLoad
import no.elg.infiniteBootleg.core.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent.Companion.occupyingLocations
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
    val halfBox2dWidth = entity.box2d.halfBox2dWidth
    val halfBox2dHeight = entity.box2d.halfBox2dHeight

    // Note: raw must be false to properly update the lights while lights are falling
    // Note 2: loadChunk must be false as the entity should then be handled as out of bounds
    val currentOccupations =
      world.getBlocksAABB(pos.blockX.toFloat(), pos.blockY.toFloat(), halfBox2dWidth, halfBox2dHeight, raw = false, loadChunk = false, includeAir = true)

    // Remove markers that are no longer occupied
    val noLongerOccupied = entity.occupyingLocations.filter { it in currentOccupations }
    entity.occupyingLocations.removeAll(noLongerOccupied)

    val newOccupation = currentOccupations.filter { it !in entity.occupyingLocations }
    for (newOccupation in newOccupation) {
      val validChunk = newOccupation.validChunkOrLoad ?: run {
        logger.error { "Failed to get valid chunk for block ${stringifyCompactLocWithChunk(newOccupation)}" }
        continue
      }
      if ((newOccupation !is EntityMarkerBlock || newOccupation.entity != entity) && newOccupation.material == Material.AIR) {
        val occupiedBlock = EntityMarkerBlock.Companion.replaceBlock(validChunk, newOccupation.localX, newOccupation.localY, entity) ?: run {
          logger.error { "Failed to replace marker block ${stringifyCompactLocWithChunk(newOccupation)}" }
          continue
        }
        entity.occupyingLocations += occupiedBlock
      }
    }
    // Remove markers after setting new markers to avoid a gap where there is no marker at all
    noLongerOccupied.forEach(EntityMarkerBlock::removeEntityMarker)
  }
}
