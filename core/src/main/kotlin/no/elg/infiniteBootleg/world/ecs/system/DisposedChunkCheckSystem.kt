package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.util.toComponentsString
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_BEFORE_EVENTS
import no.elg.infiniteBootleg.world.ecs.api.restriction.system.UniversalSystem
import no.elg.infiniteBootleg.world.ecs.components.ChunkComponent
import no.elg.infiniteBootleg.world.ecs.components.ChunkComponent.Companion.chunkComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.world.ecs.toFamily

private val logger = KotlinLogging.logger {}

object DisposedChunkCheckSystem : IteratingSystem(ChunkComponent::class.toFamily(), UPDATE_PRIORITY_BEFORE_EVENTS), UniversalSystem {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val chunkComponent = entity.chunkComponent
    val currentChunk = chunkComponent.chunk
    if (currentChunk.isDisposed) {
      val world = currentChunk.world
      val loadedChunk = world.getChunk(currentChunk.compactLocation, load = false)
      if (loadedChunk == null || loadedChunk.isDisposed) {
        logger.debug {
          val chunkLoc = stringifyCompactLoc(entity.compactBlockLoc)
          "Removing entity ${entity.id} (components: ${entity.toComponentsString()}) as it is referencing the chunk $chunkLoc; which is not loaded in the current world "
        }
        world.removeEntity(entity, Packets.DespawnEntity.DespawnReason.CHUNK_UNLOADED)
      } else {
        // Replace chunk with a loaded chunk
        chunkComponent.chunk = loadedChunk
      }
    }
  }
}
