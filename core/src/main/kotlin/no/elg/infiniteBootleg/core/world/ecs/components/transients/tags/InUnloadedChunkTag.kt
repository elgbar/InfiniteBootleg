package no.elg.infiniteBootleg.core.world.ecs.components.transients.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.TagComponent

class InUnloadedChunkTag : TagComponent {
  companion object : Mapper<InUnloadedChunkTag>() {
    var Entity.isInUnloadedChunk by tagFor<InUnloadedChunkTag>()
  }
}
