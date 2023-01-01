package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper

class OnGroundComponent : Component {

  private var contactPoints: Int = 0

  operator fun plusAssign(toAdd: Int) {
    contactPoints += toAdd
  }

  operator fun minusAssign(toRemove: Int) {
    contactPoints -= toRemove
  }

  val onGround: Boolean get() = contactPoints == 0

  companion object : Mapper<OnGroundComponent>()
}
