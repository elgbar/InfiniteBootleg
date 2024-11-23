package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.chunkBased
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world

data class ChunkComponent(private val chunkLoc: ChunkCompactLoc) : EntitySavableComponent {

  override fun hudDebug(): String = "chunk ${stringifyCompactLoc(chunkLoc)}"

  companion object : EntityLoadableMapper<ChunkComponent>() {

    /**
     * Get the chunk of this entity or null if the chunk is invalid
     *
     * @param load If the chunk should be loaded, default is **false** This is different from the default behavior of [no.elg.infiniteBootleg.world.world.World.getChunk]
     */
    fun Entity.getChunkOrNull(load: Boolean = false): Chunk? = world.getChunk(chunkComponent.chunkLoc, load)
    val Entity.chunkComponent by propertyFor(mapper)
    var Entity.chunkComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): ChunkComponent? = safeWith { ChunkComponent(protoEntity.chunk.chunkLoc) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasChunk()
  }

  override fun EntityKt.Dsl.save() {
    chunk = chunkBased {
      chunkLoc = this@ChunkComponent.chunkLoc
    }
  }
}
