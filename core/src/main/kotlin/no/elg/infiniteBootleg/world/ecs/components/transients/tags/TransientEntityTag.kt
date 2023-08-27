package no.elg.infiniteBootleg.world.ecs.components.transients.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

/**
 * This entity will not be saved in as a chunk entity
 */
class TransientEntityTag : Component {
  companion object : Mapper<TransientEntityTag>() {
    var Entity.isTransientEntity by tagFor<TransientEntityTag>()
  }
}
