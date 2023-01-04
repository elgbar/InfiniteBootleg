package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

/**
 * Whether to update the box2D position from the entities [no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent]
 */
object UpdateBox2DVelocityTag : Component, Mapper<UpdateBox2DVelocityTag>() {
  var Entity.updateBox2DVelocity by tagFor(UpdateBox2DVelocityTag)
}
