package no.elg.infiniteBootleg.world.ecs.components.transients.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

/**
 * Mark this entity as it should be destroyed, this is to ensure entities that _should_ already have be gone are properly cleaned up
 */
class ToBeDestroyedTag : Component {
  companion object : Mapper<ToBeDestroyedTag>() {
    var Entity.toBeDestroyed by tagFor<ToBeDestroyedTag>()
  }
}
