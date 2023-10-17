package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.world.ecs.api.restriction.UniversalSystem
import no.elg.infiniteBootleg.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactChunkLoc
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.InUnloadedChunkTag.Companion.isInUnloadedChunk


object NoGravityInUnloadedChunksSystem : IteratingSystem(basicStandaloneEntityFamily, UPDATE_PRIORITY_EARLY), UniversalSystem {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val isChunkLoaded = world.isChunkLoaded(entity.compactChunkLoc)
    if (!entity.isInUnloadedChunk && !isChunkLoaded) {
      if (Settings.debug) {
        Main.logger().log(
          "NoGravityInUnloadedChunksSystem",
          "Entity ${entity.id} is in unloaded chunk ${stringifyCompactLoc(entity.compactBlockLoc)}, disabling gravity"
        )
      }
      entity.isInUnloadedChunk = true
      entity.box2d.disableGravity()
    } else if (entity.isInUnloadedChunk && isChunkLoaded) {
      entity.isInUnloadedChunk = false
      if (Settings.debug) {
        Main.logger().log(
          "NoGravityInUnloadedChunksSystem",
          "Entity ${entity.id} is now in a loaded chunk ${stringifyCompactLoc(entity.compactBlockLoc)}, enabling gravity (if not flying)"
        )
      }
      if (!entity.flying) {
        entity.box2d.enableGravity()
      }
    }
  }
}
