package no.elg.infiniteBootleg.world.ecs.components.additional

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.math.component1
import ktx.math.component2
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.api.AdditionalComponentsLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.AdditionalComponentsSavableComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position

class ChunkComponent(val chunk: Chunk) : AdditionalComponentsSavableComponent {

  companion object : AdditionalComponentsLoadableMapper<ChunkComponent, Chunk>() {
    val Entity.chunk get() = chunkComponent.chunk
    val Entity.chunkOrNull get() = chunkComponentOrNull?.chunk
    val Entity.chunkComponent by propertyFor(ChunkComponent.mapper)
    var Entity.chunkComponentOrNull by optionalPropertyFor(ChunkComponent.mapper)
    val Entity.block: Block
      get() {
        val (worldX, worldY) = this.position
        return chunk.getBlock(worldX.toInt().chunkOffset(), worldY.toInt().chunkOffset())
      }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.AdditionalComponents, state: Chunk): ChunkComponent = with(ChunkComponent(state))
    override fun ProtoWorld.Entity.AdditionalComponents.checkShouldLoad(): Boolean = hasChunk()
  }

  override fun EntityKt.AdditionalComponentsKt.Dsl.save() {
    chunk = true
  }
}
