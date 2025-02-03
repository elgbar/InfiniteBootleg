package no.elg.infiniteBootleg.core.world.ecs.system

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.core.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.compactChunkLoc
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.InUnloadedChunkTag.Companion.isInUnloadedChunk
import no.elg.infiniteBootleg.core.world.ecs.system.api.AuthorizedEntitiesIteratingSystem

private val logger = KotlinLogging.logger {}

// TODO refactor this system to be NoMovementInUnlockedChunksSystem, disabling all movement and setting velocity to 0
object NoGravityInUnloadedChunksSystem : AuthorizedEntitiesIteratingSystem(basicStandaloneEntityFamily, UPDATE_PRIORITY_EARLY) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val isChunkLoaded = world.isChunkLoaded(entity.compactChunkLoc)
    if (!entity.isInUnloadedChunk && !isChunkLoaded) {
      if (Settings.debug) {
        logger.info {
          "Entity ${entity.id} is in unloaded chunk ${stringifyCompactLoc(entity.compactBlockLoc)}, disabling gravity"
        }
      }
      entity.isInUnloadedChunk = true
      entity.box2d.disableGravity()
    } else if (entity.isInUnloadedChunk && isChunkLoaded) {
      entity.isInUnloadedChunk = false
      if (Settings.debug) {
        logger.info {
          "Entity ${entity.id} is now in a loaded chunk ${stringifyCompactLoc(entity.compactBlockLoc)}, enabling gravity (if not flying)"
        }
      }
      if (!entity.flying) {
        entity.box2d.enableGravity()
      }
    }
  }
}
