package no.elg.infiniteBootleg.world.ecs.components.transients

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.math.component1
import ktx.math.component2
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position

class ChunkComponent(val chunk: Chunk) : Component {
  companion object : Mapper<ChunkComponent>() {
    val Entity.chunk get() = chunkComponent.chunk
    val Entity.chunkOrNull get() = chunkComponentOrNull?.chunk
    val Entity.chunkComponent by propertyFor(ChunkComponent.mapper)
    var Entity.chunkComponentOrNull by optionalPropertyFor(ChunkComponent.mapper)
    val Entity.block: Block
      get() {
        val (worldX, worldY) = this.position
        return chunk.getBlock(worldX.toInt().chunkOffset(), worldY.toInt().chunkOffset())
      }
  }
}
