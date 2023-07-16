package no.elg.infiniteBootleg.world.ecs.components.transients.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

/**
 * Whether to update the box2D position from the entities [no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent]
 */
class UpdateBox2DVelocityTag : Component {
  companion object : Mapper<UpdateBox2DVelocityTag>() {
    var Entity.updateBox2DVelocity by tagFor<UpdateBox2DVelocityTag>()
  }
}
