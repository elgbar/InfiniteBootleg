package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.util.toComponentsString
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_BEFORE_EVENTS
import no.elg.infiniteBootleg.world.ecs.api.restriction.UniversalSystem
import no.elg.infiniteBootleg.world.ecs.components.ChunkComponent
import no.elg.infiniteBootleg.world.ecs.components.ChunkComponent.Companion.chunkComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.world.ecs.toFamily

object DisposedChunkCheckSystem : IteratingSystem(ChunkComponent::class.toFamily(), UPDATE_PRIORITY_BEFORE_EVENTS), UniversalSystem {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val chunkComponent = entity.chunkComponent
    val currentChunk = chunkComponent.chunk
    if (currentChunk.isDisposed) {
      val world = currentChunk.world
      val loadedChunk = world.getChunk(currentChunk.compactLocation, load = false)
      if (loadedChunk == null || loadedChunk.isDisposed) {
        Main.logger().debug("DisposedChunkCheckSystem") {
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
