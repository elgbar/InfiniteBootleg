package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper
import no.elg.infiniteBootleg.input.KeyboardControls

data class VelocityComponent(
  val dx: Double,
  val dy: Double,
  val maxDx: Double = KeyboardControls.MAX_X_VEL.toDouble(),
  val maxDy: Double = KeyboardControls.MAX_X_VEL.toDouble()
) : Component {

  init {
    require(maxDx > 0) { "Max dx velocity must be strictly positive" }
    require(maxDy > 0) { "Max dy velocity must be strictly positive" }
  }

  companion object : Mapper<VelocityComponent>()
}
