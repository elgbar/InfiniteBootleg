package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor

class GroundedComponent : Component {

  private var contactPoints: Int = 0

  operator fun plusAssign(toAdd: Int) {
    contactPoints += toAdd
  }

  operator fun minusAssign(toRemove: Int) {
    contactPoints -= toRemove
  }

  val onGround: Boolean get() = contactPoints == 0

  companion object : Mapper<GroundedComponent>() {

    var Entity.grounded by propertyFor(mapper)
    var Entity.groundedOrNull by optionalPropertyFor(mapper)
  }
}
