package no.elg.infiniteBootleg.world.ecs.components.transients.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor
import no.elg.infiniteBootleg.world.ecs.api.restriction.component.TagComponent

/**
 * Mark this entity as it should be destroyed, this is to ensure entities that _should_ already have be gone are properly cleaned up
 */
class ToBeDestroyedTag : TagComponent {
  companion object : Mapper<ToBeDestroyedTag>() {
    var Entity.toBeDestroyed by tagFor<ToBeDestroyedTag>()
  }
}
