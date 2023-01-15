package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

/**
 * Whether to update the box2D position from the entities [no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent]
 */
class UpdateBox2DPositionTag : Component {
  companion object : Mapper<UpdateBox2DPositionTag>() {
    var Entity.updateBox2DPosition by tagFor<UpdateBox2DPositionTag>()
  }
}
