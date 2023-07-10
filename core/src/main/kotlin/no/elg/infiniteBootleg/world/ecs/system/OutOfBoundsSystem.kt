package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.world.ecs.basicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactChunkLoc
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world

object OutOfBoundsSystem : IteratingSystem(basicEntityFamily, UPDATE_PRIORITY_EARLY) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    if (!world.isChunkLoaded(entity.compactChunkLoc)) {
      Main.logger().log("OutOfBoundsSystem", "Entity ${entity.nameOrNull} is out of bounds at ${stringifyCompactLoc(entity.compactBlockLoc)}")
      world.engine.removeEntity(entity)
    }
  }
}
