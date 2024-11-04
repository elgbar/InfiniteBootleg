package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.removeSelf
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.invalid
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.valid
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.StatefulEntityLoadableMapper

data class ChunkComponent(var chunk: Chunk) : EntitySavableComponent {

  /**
   * Validate the chunk and return if the chunk is valid.
   *
   * If there is no longer a valid chunk at the current location, the entity will be removed
   */
  fun validateChunk(entity: Entity): Boolean {
    if (chunk.invalid()) {
      val newChunk = chunk.world.getChunk(chunk.compactLocation, load = false)
      if (newChunk.valid()) {
        chunk = newChunk
        return true
      } else {
        entity.removeSelf(DespawnReason.CHUNK_UNLOADED)
        return false
      }
    }
    return true
  }

  override fun hudDebug(): String = "chunk ${stringifyCompactLoc(chunk)}"

  companion object : StatefulEntityLoadableMapper<ChunkComponent, Chunk>() {

    /**
     * Get the chunk of this entity or null if the chunk is invalid
     */
    val Entity.chunkOrNull: Chunk?
      get() {
        val comp = chunkComponentOrNull ?: return null
        comp.validateChunk(this)
        return comp.chunk
      }
    val Entity.chunkComponent by propertyFor(mapper)
    var Entity.chunkComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity, state: Chunk): ChunkComponent? = safeWith { ChunkComponent(state) }
    override fun ProtoWorld.Entity.checkShouldLoad(state: () -> Chunk): Boolean = hasChunk()
    val PROTO_CHUNK_BASED: ProtoWorld.Entity.ChunkBased = ProtoWorld.Entity.ChunkBased.getDefaultInstance()
  }

  override fun EntityKt.Dsl.save() {
    chunk = PROTO_CHUNK_BASED
  }
}
