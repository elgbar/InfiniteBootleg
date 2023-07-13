package no.elg.infiniteBootleg.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.util.worldCompactToChunk
import no.elg.infiniteBootleg.world.Block.Companion.removeAsync
import no.elg.infiniteBootleg.world.Block.Companion.worldX
import no.elg.infiniteBootleg.world.Block.Companion.worldY
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.material
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.createFallingBlockEntity
import no.elg.infiniteBootleg.world.ecs.gravityAffectedBlockFamily

object FallingBlockSystem : IteratingSystem(gravityAffectedBlockFamily, UPDATE_PRIORITY_DEFAULT) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val pos = entity.positionComponent
    val locBelow = Location.relativeCompact(pos.blockX, pos.blockY, Direction.SOUTH)

    if (world.isChunkLoaded(locBelow.worldCompactToChunk()) && world.isAirBlock(locBelow)) {
      val block = world.getRawBlock(pos.blockX, pos.blockY, false) ?: return
      block.removeAsync()
      world.engine.createFallingBlockEntity(world, block.worldX + 0.5f, block.worldY + 0.5f, 0f, -3f, entity.material)
    }
  }
}
