package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.core.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.compactChunkLoc
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.tags.CanBeOutOfBoundsTag.Companion.canBeOutOfBounds
import no.elg.infiniteBootleg.protobuf.Packets

private val logger = KotlinLogging.logger {}

object OutOfBoundsSystem : IteratingSystem(basicStandaloneEntityFamily, UPDATE_PRIORITY_EARLY) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val compactedChunkLoc = entity.compactChunkLoc
    if (!entity.canBeOutOfBounds && world.render.isOutOfView(compactedChunkLoc) && !world.isChunkLoaded(compactedChunkLoc)) {
      if (Settings.debug) {
        logger.info { "Entity ${entity.id} is out of bounds at ${stringifyCompactLoc(entity.compactBlockLoc)}" }
      }
      world.removeEntity(entity, Packets.DespawnEntity.DespawnReason.CHUNK_UNLOADED)
    }
  }
}
