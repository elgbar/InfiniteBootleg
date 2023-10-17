package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.allOf
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_BEFORE_EVENTS
import no.elg.infiniteBootleg.world.ecs.api.restriction.UniversalSystem
import no.elg.infiniteBootleg.world.ecs.components.ChunkComponent
import no.elg.infiniteBootleg.world.ecs.components.ChunkComponent.Companion.chunk
import no.elg.infiniteBootleg.world.ecs.components.ChunkComponent.Companion.chunkComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactChunkLoc
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world

object DisposedChunkCheckSystem : IteratingSystem(allOf(ChunkComponent::class).get(), UPDATE_PRIORITY_BEFORE_EVENTS), UniversalSystem {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val entityChunk = entity.chunk
    val loadedChunk = world.getLoadedChunk(entity.compactChunkLoc)
    if (entityChunk.isDisposed) {
      if (loadedChunk == null || loadedChunk.isDisposed) {
        if (Settings.debug) {
          Main.logger().debug(
            "DisposedChunkCheckSystem",
            "Entity ${entity.id} is out of bounds at ${stringifyCompactLoc(entity.compactBlockLoc)} (components: ${entity.components.joinToString()})"
          )
        }
        world.removeEntity(entity)
      } else {
        // Replace chunk with a loaded chunk
        entity.chunkComponent.chunk = loadedChunk
      }
    }
  }
}
