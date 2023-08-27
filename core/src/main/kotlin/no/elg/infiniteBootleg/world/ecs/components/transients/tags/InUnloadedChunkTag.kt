package no.elg.infiniteBootleg.world.ecs.components.transients.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

class InUnloadedChunkTag : Component {
  companion object : Mapper<InUnloadedChunkTag>() {
    var Entity.isInUnloadedChunk by tagFor<InUnloadedChunkTag>()
  }
}
