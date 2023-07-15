package no.elg.infiniteBootleg.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import ktx.collections.plusAssign
import ktx.collections.removeAll
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.block.OccupyingBlocksComponent.Companion.occupyingLocations
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.fallingBlockFamily

/**
 * Sets a marker block in the world to indicate that an entity is occupying that block
 */
object UpdateGridBlockSystem : IteratingSystem(fallingBlockFamily, UPDATE_PRIORITY_DEFAULT) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val pos = entity.positionComponent
    val halfBox2dWidth = entity.box2d.halfBox2dWidth
    val halfBox2dHeight = entity.box2d.halfBox2dHeight

    val currentOccupations = world.getBlocksAABB(pos.blockX.toFloat(), pos.blockY.toFloat(), halfBox2dWidth, halfBox2dHeight, raw = false, loadChunk = true, includeAir = true)
    entity.occupyingLocations.filter { it !in currentOccupations }.forEach(EntityMarkerBlock::removeEntityMarker)
    entity.occupyingLocations.removeAll { it !in currentOccupations }

    for (currentOccupation in currentOccupations) {
      if ((currentOccupation !is EntityMarkerBlock || currentOccupation.entity != entity) && currentOccupation.material == Material.AIR) {
        val block = EntityMarkerBlock.fromOtherBlock(currentOccupation, entity).let { block ->
          world.setBlock(currentOccupation.worldX, currentOccupation.worldY, block, updateTexture = true)
        } ?: continue
        entity.occupyingLocations += block as EntityMarkerBlock
      }
    }
  }
}
