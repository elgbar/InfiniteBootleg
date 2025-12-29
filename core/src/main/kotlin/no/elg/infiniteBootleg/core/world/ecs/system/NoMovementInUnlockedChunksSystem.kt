package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.core.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.compactChunkLoc
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FlyingTag.Companion.ensureFlyingStatus
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.InUnloadedChunkTag.Companion.isInUnloadedChunk
import no.elg.infiniteBootleg.core.world.ecs.system.api.AuthorizedEntitiesIteratingSystem

private val logger = KotlinLogging.logger {}

object NoMovementInUnlockedChunksSystem : AuthorizedEntitiesIteratingSystem(basicStandaloneEntityFamily, UPDATE_PRIORITY_EARLY) {

  fun stopMovement(entity: Entity) {
    entity.setVelocity(0f, 0f)
    entity.box2d.disableGravity()
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val isChunkLoaded = world.isChunkLoaded(entity.compactChunkLoc)
    val hasTag = entity.isInUnloadedChunk
    if (!hasTag && !isChunkLoaded) {
      if (Settings.debug) {
        logger.info {
          "Entity ${entity.id} is in unloaded chunk ${stringifyCompactLoc(entity.compactBlockLoc)}, disabling gravity"
        }
      }
      entity.isInUnloadedChunk = true
      stopMovement(entity)
    } else if (hasTag) {
      if (isChunkLoaded) {
        entity.isInUnloadedChunk = false
        entity.ensureFlyingStatus()
        if (Settings.debug) {
          logger.info {
            "Entity ${entity.id} is now in a loaded chunk ${stringifyCompactLoc(entity.compactBlockLoc)}, enabling gravity (if not flying)"
          }
        }
      } else if (entity.velocityComponent.isMoving()) {
        logger.debug { "Entity ${entity.id} is in unloaded chunk but was moving!" }
        stopMovement(entity)
      }
    }
  }
}
