package no.elg.infiniteBootleg.world.ecs.components.block

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.world.chunks.Chunk

class ChunkComponent(val chunk: Chunk) : Component {
  companion object : Mapper<ChunkComponent>() {
    val Entity.chunk get() = chunkComponent.chunk
    val Entity.chunkOrNull get() = chunkComponentOrNull?.chunk
    val Entity.chunkComponent by propertyFor(ChunkComponent.mapper)
    var Entity.chunkComponentOrNull by optionalPropertyFor(ChunkComponent.mapper)
  }
}
