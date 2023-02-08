package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.collections.GdxLongArray

class GroundedComponent : Component {

  val feetContacts: GdxLongArray = GdxLongArray(false, 16)
  val leftArmContacts: GdxLongArray = GdxLongArray(false, 16)
  val rightArmContacts: GdxLongArray = GdxLongArray(false, 16)

  val onGround: Boolean get() = !feetContacts.isEmpty
  val canMoveLeft: Boolean get() = onGround || leftArmContacts.isEmpty || !rightArmContacts.isEmpty
  val canMoveRight: Boolean get() = onGround || rightArmContacts.isEmpty || !leftArmContacts.isEmpty

  fun canMove(dir: Float): Boolean = when {
    dir < 0 -> canMoveLeft
    dir > 0 -> canMoveRight
    else -> true
  }

  companion object : Mapper<GroundedComponent>() {

    var Entity.grounded by propertyFor(mapper)
    var Entity.groundedOrNull by optionalPropertyFor(mapper)
  }
}
