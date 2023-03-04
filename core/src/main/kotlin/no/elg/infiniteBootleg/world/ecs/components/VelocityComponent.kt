package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.input.KeyboardControls.Companion.MAX_X_VEL
import no.elg.infiniteBootleg.input.KeyboardControls.Companion.MAX_Y_VEL
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.vector2f

data class VelocityComponent(
  var dx: Float,
  var dy: Float,
  var maxDx: Float = MAX_X_VEL,
  var maxDy: Float = MAX_Y_VEL
) : Component {

  fun toVector2f(): ProtoWorld.Vector2f = vector2f {
    x = this@VelocityComponent.dx
    y = this@VelocityComponent.dy
  }

  init {
    require(maxDx > 0) { "Max dx velocity must be strictly positive" }
    require(maxDy > 0) { "Max dy velocity must be strictly positive" }
  }

  companion object : Mapper<VelocityComponent>() {
    var Entity.velocity by propertyFor(mapper)
    var Entity.velocityOrNull by optionalPropertyFor(mapper)
  }
}
