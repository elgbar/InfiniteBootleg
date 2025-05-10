package no.elg.infiniteBootleg.core.world.ecs.components.transients.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.TagComponent

/**
 * This entity should react to events that happened near it
 */
class ReactToEventTag : TagComponent {
  companion object : Mapper<ReactToEventTag>() {
    var Entity.reactToEventTag by tagFor<ReactToEventTag>()
  }
}
