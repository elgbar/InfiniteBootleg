package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactChunkLoc
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.CanBeOutOfBoundsTag.Companion.canBeOutOfBounds
import no.elg.infiniteBootleg.world.ecs.system.restriction.DuplexSystem

object OutOfBoundsSystem : IteratingSystem(basicStandaloneEntityFamily, UPDATE_PRIORITY_EARLY), DuplexSystem {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val compactedChunkLoc = entity.compactChunkLoc
    if (!entity.canBeOutOfBounds && world.render.isOutOfView(compactedChunkLoc) && !world.isChunkLoaded(compactedChunkLoc)) {
      if (Settings.debug) {
        Main.logger().log("OutOfBoundsSystem", "Entity ${entity.id} is out of bounds at ${stringifyCompactLoc(entity.compactBlockLoc)}")
      }
      world.removeEntity(entity)
    }
  }
}
