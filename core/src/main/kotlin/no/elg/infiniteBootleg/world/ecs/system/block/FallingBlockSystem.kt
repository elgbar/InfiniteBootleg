package no.elg.infiniteBootleg.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.collections.plusAssign
import no.elg.infiniteBootleg.screens.hud.DebugText.pos
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.chunkOffsetX
import no.elg.infiniteBootleg.util.chunkOffsetY
import no.elg.infiniteBootleg.util.isAir
import no.elg.infiniteBootleg.util.relativeCompact
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.util.stringifyCompactLocWithChunk
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.blocks.Block.Companion.validChunk
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_LATE
import no.elg.infiniteBootleg.world.ecs.api.restriction.system.AuthoritativeSystem
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.material
import no.elg.infiniteBootleg.world.ecs.components.OccupyingBlocksComponent.Companion.occupyingLocations
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.getChunkOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag.Companion.gravityAffected
import no.elg.infiniteBootleg.world.ecs.creation.createFallingBlockStandaloneEntity
import no.elg.infiniteBootleg.world.ecs.gravityAffectedBlockFamily

private val logger = KotlinLogging.logger {}

/**
 * About the priority: We want this to run after the [UpdateGridBlockSystem] so that the block is properly removed
 */
object FallingBlockSystem : IteratingSystem(gravityAffectedBlockFamily, UPDATE_PRIORITY_LATE), AuthoritativeSystem {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val chunk = entity.getChunkOrNull() ?: run {
      logger.error { "Failed to get chunk of block at ${stringifyCompactLoc(entity.positionComponent)}" }
      return
    }
    val material = entity.material
    val pos = entity.positionComponent
    val world = chunk.world
    val locBelow = relativeCompact(pos.blockX, pos.blockY, Direction.SOUTH)
    val isAirBelow = if (locBelow.worldToChunk() == chunk.compactLocation) {
      chunk.getRawBlock(locBelow.chunkOffsetX(), locBelow.chunkOffsetY()).isAir()
    } else {
      world.isAirBlock(locBelow, loadChunk = false)
    }
    if (isAirBelow) {
      val block = chunk.getRawBlock(pos.blockX.chunkOffset(), pos.blockY.chunkOffset()) ?: run {
        logger.warn { "Failed to get block at ${stringifyCompactLoc(pos)}" }
        return
      }
      entity.gravityAffected = false // Prevent the block to fall multiple times
      world.engine.createFallingBlockStandaloneEntity(world, block.worldX + 0.5f, block.worldY + 0.5f, 0f, 0f, material) { fallingEntity ->
        val validChunk = block.validChunk ?: run {
          logger.error { "Failed to get valid chunk for block ${stringifyCompactLocWithChunk(block)}" }
          entity.gravityAffected = true // If we failed to fall, we want it to fall in the future
          return@createFallingBlockStandaloneEntity false
        }
        val replacedBlock = EntityMarkerBlock.replaceBlock(validChunk, block.localX, block.localY, fallingEntity) ?: run {
          logger.error { "Failed to get replace block ${stringifyCompactLocWithChunk(block)} with EMB" }
          entity.gravityAffected = true
          return@createFallingBlockStandaloneEntity false
        }
        fallingEntity.occupyingLocations += replacedBlock
        true
      }
    }
  }
}
