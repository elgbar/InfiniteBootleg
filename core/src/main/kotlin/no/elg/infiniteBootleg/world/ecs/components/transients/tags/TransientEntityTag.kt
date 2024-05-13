package no.elg.infiniteBootleg.world.ecs.components.transients.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor
import no.elg.infiniteBootleg.world.ecs.api.restriction.component.TagComponent

/**
 * This entity will not be saved in as a chunk entity
 */
class TransientEntityTag : TagComponent {
  companion object : Mapper<TransientEntityTag>() {
    var Entity.isTransientEntity by tagFor<TransientEntityTag>()
  }
}
