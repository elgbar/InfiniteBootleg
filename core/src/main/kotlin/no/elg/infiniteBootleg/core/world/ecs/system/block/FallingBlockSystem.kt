package no.elg.infiniteBootleg.core.world.ecs.system.block

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.collections.plusAssign
import no.elg.infiniteBootleg.core.events.BlockChangedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.api.RegisteredEventListener
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.chunkOffset
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.isAir
import no.elg.infiniteBootleg.core.util.relativeCompact
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.util.stringifyCompactLocWithChunk
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.validChunk
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.core.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_LATE
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.system.AuthoritativeSystem
import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent.Companion.material
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent.Companion.occupyingLocations
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.getChunkOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.ReactToEventTag.Companion.reactToEventTag
import no.elg.infiniteBootleg.core.world.ecs.creation.createFallingBlockStandaloneEntity
import no.elg.infiniteBootleg.core.world.ecs.gravityAffectedBlockFamily
import no.elg.infiniteBootleg.core.world.ecs.gravityAffectedBlockFamilyActive

private val logger = KotlinLogging.logger {}

/**
 * About the priority: We want this to run after the [UpdateGridBlockSystem] so that the block is properly removed
 */
class FallingBlockSystem :
  IteratingSystem(gravityAffectedBlockFamilyActive, UPDATE_PRIORITY_LATE),
  AuthoritativeSystem {

  private fun makeBlockFallListener(): RegisteredEventListener =
    EventManager.registerListener<BlockChangedEvent> { event ->
      val oldBlock = event.oldBlock ?: return@registerListener
      if (event.newBlock.isAir()) {
        val (blockX: WorldCoord, blockY: WorldCoord) = oldBlock.compactWorldLoc
        val chunk = oldBlock.chunk
        val (aboveX: WorldCoord, aboveY: WorldCoord) = relativeCompact(blockX, blockY, Direction.NORTH)
        chunk.world.actionOnChunk(chunk, aboveX, aboveY, loadChunk = false) { actionChunk ->
          val aboveEntity = actionChunk?.getRawBlock(aboveX.chunkOffset(), aboveY.chunkOffset())?.entity ?: return@actionOnChunk
          val isGravityAffectedEntity = gravityAffectedBlockFamily.matches(aboveEntity)
          if (isGravityAffectedEntity) {
//            logger.warn { "Block @ ${stringifyCompactLoc(aboveX, aboveY)} will try to fall down" }
            aboveEntity.reactToEventTag = true
          }
        }
      }
    }

  private var blockChangedEventListener: RegisteredEventListener? = null

  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    blockChangedEventListener = makeBlockFallListener()
  }

  override fun removedFromEngine(engine: Engine) {
    super.removedFromEngine(engine)
    blockChangedEventListener?.removeListener()
    blockChangedEventListener = null
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val chunk = entity.getChunkOrNull() ?: run {
      logger.trace {
        "Failed to get chunk of block at ${stringifyCompactLocWithChunk(entity.positionComponent)}, chunk was probably unloaded during iteration"
      }
      return
    }
    val world = chunk.world
    val pos = entity.positionComponent
    val (belowX, belowY) = relativeCompact(pos.blockX, pos.blockY, Direction.SOUTH)
    val isAirBelow = world.actionOnChunk(chunk, belowX, belowY, loadChunk = false) { maybeActionChunk ->
      val actionChunk = maybeActionChunk ?: return@actionOnChunk false
      actionChunk.getRawBlock(belowX.chunkOffset(), belowY.chunkOffset()).isAir()
    }
    if (isAirBelow) {
      val block = chunk.getRawBlock(pos.blockX.chunkOffset(), pos.blockY.chunkOffset()) ?: run {
        logger.warn { "Failed to get block at ${stringifyCompactLoc(pos)}" }
        return
      }

      entity.reactToEventTag = false
      world.engine.createFallingBlockStandaloneEntity(world, block.worldX + 0.5f, block.worldY + 0.5f, 0f, 0f, entity.material) { fallingEntity ->
        val validChunk = block.validChunk ?: run {
          logger.error { "Failed to get valid chunk for block ${stringifyCompactLocWithChunk(block)}" }
          entity.reactToEventTag = true // If we failed to fall, we want it to fall in the future
          return@createFallingBlockStandaloneEntity false
        }
        val replacedBlock = EntityMarkerBlock.Companion.replaceBlock(validChunk, block.localX, block.localY, fallingEntity) ?: run {
          logger.error { "Failed to get replace block ${stringifyCompactLocWithChunk(block)} with EMB" }
          entity.reactToEventTag = true
          return@createFallingBlockStandaloneEntity false
        }
        fallingEntity.occupyingLocations += replacedBlock
        true
      }
    }
  }
}
