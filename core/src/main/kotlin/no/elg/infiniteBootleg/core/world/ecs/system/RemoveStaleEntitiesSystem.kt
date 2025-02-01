package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.util.isBeingRemoved
import no.elg.infiniteBootleg.core.util.removeSelf
import no.elg.infiniteBootleg.core.util.toComponentsString
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_LAST
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.system.UniversalSystem
import no.elg.infiniteBootleg.core.world.ecs.staleEntityFamily
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason

private val logger = KotlinLogging.logger {}

object RemoveStaleEntitiesSystem : IteratingSystem(staleEntityFamily, UPDATE_PRIORITY_LAST), UniversalSystem {

  private val seenEntities = HashSet<Entity>()

  override fun processEntity(entity: Entity, deltaTime: Float) {
    if (entity.isBeingRemoved) {
      return
    }
    if (entity in seenEntities) {
      logger.warn { "Seen a stale entity with components ${entity.toComponentsString()}" }
      entity.removeSelf(DespawnReason.UNKNOWN_ENTITY)
    } else {
      seenEntities += entity
    }
  }
}
