package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.isBeingRemoved
import no.elg.infiniteBootleg.util.toComponentsString
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.world.ecs.api.restriction.system.UniversalSystem
import no.elg.infiniteBootleg.world.ecs.staleEntityFamily

object RemoveStaleEntitiesSystem : IteratingSystem(staleEntityFamily, UPDATE_PRIORITY_LAST), UniversalSystem {

  private val seenEntities = HashSet<Entity>()

  override fun processEntity(entity: Entity, deltaTime: Float) {
    if (entity.isBeingRemoved) {
      return
    }
    if (entity in seenEntities) {
      Main.logger().warn("RemoveStaleEntitiesSystem", "Seen a stale entity with components ${entity.toComponentsString()}")
      engine.removeEntity(entity)
    } else {
      seenEntities += entity
    }
  }
}
