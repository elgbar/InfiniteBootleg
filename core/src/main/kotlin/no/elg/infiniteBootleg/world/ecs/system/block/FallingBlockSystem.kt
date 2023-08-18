package no.elg.infiniteBootleg.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.util.relativeCompact
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_LATE
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.materialOrNull
import no.elg.infiniteBootleg.world.ecs.components.additional.OccupyingBlocksComponent.Companion.occupyingLocations
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag.Companion.gravityAffected
import no.elg.infiniteBootleg.world.ecs.creation.createFallingBlockStandaloneEntity
import no.elg.infiniteBootleg.world.ecs.gravityAffectedBlockFamily

/**
 * About the priority: We want this to run after the [UpdateGridBlockSystem] so that the block is properly removed
 */
object FallingBlockSystem : IteratingSystem(gravityAffectedBlockFamily, UPDATE_PRIORITY_LATE) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val material = entity.materialOrNull ?: return
    val world = entity.world
    val pos = entity.positionComponent
    val locBelow = relativeCompact(pos.blockX, pos.blockY, Direction.SOUTH)

    if (world.isChunkLoaded(locBelow.worldToChunk()) && world.isAirBlock(locBelow)) {
      val block = world.getRawBlock(pos.blockX, pos.blockY, false) ?: return
      entity.gravityAffected = false // Prevent the block to fall multiple times
      world.engine.createFallingBlockStandaloneEntity(world, block.worldX + 0.5f, block.worldY + 0.5f, 0f, 0f, material) {
        it.occupyingLocations.add(EntityMarkerBlock.replaceBlock(block, it))
      }
    }
  }
}
