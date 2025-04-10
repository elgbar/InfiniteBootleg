package no.elg.infiniteBootleg.core.world.ecs.components.transients.tags

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.TagComponent

/**
 * Whether to update the box2D position from the entities [no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent]
 */
class UpdateBox2DVelocityTag :
  AuthoritativeOnlyComponent,
  TagComponent {
  companion object : Mapper<UpdateBox2DVelocityTag>() {
    var Entity.updateBox2DVelocity by tagFor<UpdateBox2DVelocityTag>()
  }
}
