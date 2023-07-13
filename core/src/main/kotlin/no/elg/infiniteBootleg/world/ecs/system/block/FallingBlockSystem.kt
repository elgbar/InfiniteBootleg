package no.elg.infiniteBootleg.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.remove
import no.elg.infiniteBootleg.util.worldCompactToChunk
import no.elg.infiniteBootleg.world.Block.Companion.remove
import no.elg.infiniteBootleg.world.Block.Companion.worldX
import no.elg.infiniteBootleg.world.Block.Companion.worldY
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.materialOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag
import no.elg.infiniteBootleg.world.ecs.createFallingBlockStandaloneEntity
import no.elg.infiniteBootleg.world.ecs.gravityAffectedBlockFamily

object FallingBlockSystem : IteratingSystem(gravityAffectedBlockFamily, UPDATE_PRIORITY_DEFAULT) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val material = entity.materialOrNull ?: return
    val world = entity.world
    val pos = entity.positionComponent
    val locBelow = Location.relativeCompact(pos.blockX, pos.blockY, Direction.SOUTH)

    if (world.isChunkLoaded(locBelow.worldCompactToChunk()) && world.isAirBlock(locBelow)) {
      val block = world.getRawBlock(pos.blockX, pos.blockY, false) ?: return
      entity.remove<GravityAffectedTag>() // Prevent the block to fall multiple times
      block.remove()
      world.engine.createFallingBlockStandaloneEntity(world, block.worldX + 0.5f, block.worldY + 0.5f, 0f, 0f, material)
    }
  }
}