package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_EARLY
import no.elg.infiniteBootleg.world.ecs.blockEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.additional.ChunkComponent.Companion.chunk
import no.elg.infiniteBootleg.world.ecs.components.additional.ChunkComponent.Companion.chunkComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactChunkLoc
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world

object DisposedChunkCheckSystem : IteratingSystem(blockEntityFamily, UPDATE_PRIORITY_EARLY) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world
    val entityChunk = entity.chunk
    val loadedChunk = world.getLoadedChunk(entity.compactChunkLoc)
    if (entityChunk.isDisposed) {
      if (loadedChunk == null || loadedChunk.isDisposed) {
        if (Settings.debug) {
          Main.logger().debug("DisposedChunkCheckSystem", "Entity ${entity.id} is out of bounds at ${stringifyCompactLoc(entity.compactBlockLoc)}")
        }
        world.engine.removeEntity(entity)
      } else {
        // Replace chunk with a loaded chunk
        entity.chunkComponent.chunk = loadedChunk
      }
    }
  }
}
