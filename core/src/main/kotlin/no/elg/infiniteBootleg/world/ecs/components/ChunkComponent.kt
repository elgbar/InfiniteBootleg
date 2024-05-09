package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.math.component1
import ktx.math.component2
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.StatefulEntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position

data class ChunkComponent(var chunk: Chunk) : EntitySavableComponent {

  companion object : StatefulEntityLoadableMapper<ChunkComponent, Chunk>() {
    /**
     * Chunk might be disposed, make sure to check it when calling this
     */
    val Entity.chunk get() = chunkComponent.chunk
    val Entity.chunkOrNull get() = chunkComponentOrNull?.chunk
    val Entity.chunkComponent by propertyFor(ChunkComponent.mapper)
    var Entity.chunkComponentOrNull by optionalPropertyFor(ChunkComponent.mapper)
    val Entity.block: Block
      get() {
        val (worldX, worldY) = this.position
        return chunk.getBlock(worldX.toInt().chunkOffset(), worldY.toInt().chunkOffset())
      }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity, state: Chunk): ChunkComponent? = safeWith { ChunkComponent(state) }
    override fun ProtoWorld.Entity.checkShouldLoad(state: () -> Chunk): Boolean = hasChunk()
    val PROTO_CHUNK_BASED: ProtoWorld.Entity.ChunkBased = ProtoWorld.Entity.ChunkBased.getDefaultInstance()
  }

  override fun EntityKt.Dsl.save() {
    chunk = PROTO_CHUNK_BASED
  }
}
