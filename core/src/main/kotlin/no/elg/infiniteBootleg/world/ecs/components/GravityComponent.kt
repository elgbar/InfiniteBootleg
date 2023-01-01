package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper
import no.elg.infiniteBootleg.world.box2d.WorldBody

/**
 * The entity is affected by gravity
 */
data class GravityComponent(var xForce: Double = WorldBody.X_WORLD_GRAVITY.toDouble(), var yForce: Double = WorldBody.Y_WORLD_GRAVITY.toDouble()) : Component {
  companion object : Mapper<GravityComponent>()
}
