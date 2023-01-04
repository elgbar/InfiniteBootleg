package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.world.box2d.WorldBody

/**
 * The entity is affected by gravity
 */
data class GravityComponent(
  val xForce: Double = WorldBody.X_WORLD_GRAVITY.toDouble(),
  val yForce: Double = WorldBody.Y_WORLD_GRAVITY.toDouble()
) : Component {
  companion object : Mapper<GravityComponent>() {
    var Entity.gravity by propertyFor(mapper)
    var Entity.gravityOrNull by optionalPropertyFor(mapper)
  }
}
