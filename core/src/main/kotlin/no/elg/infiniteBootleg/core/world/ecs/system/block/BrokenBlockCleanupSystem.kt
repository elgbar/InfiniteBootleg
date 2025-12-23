package no.elg.infiniteBootleg.core.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.brokenBlockFamily
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world

object BrokenBlockCleanupSystem : IteratingSystem(brokenBlockFamily, UPDATE_PRIORITY_DEFAULT) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val pos = entity.positionComponent
    world.removeBlock(pos.blockX, pos.blockY)
  }
}
