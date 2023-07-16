package no.elg.infiniteBootleg.world.ecs.components.transients

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.world.box2d.LongContactTracker
import no.elg.infiniteBootleg.world.ecs.creation.PLAYERS_FOOT_USER_DATA
import no.elg.infiniteBootleg.world.ecs.creation.PLAYERS_LEFT_ARM_USER_DATA
import no.elg.infiniteBootleg.world.ecs.creation.PLAYERS_RIGHT_ARM_USER_DATA

class GroundedComponent : Component {

  val feetContacts = LongContactTracker(PLAYERS_FOOT_USER_DATA)
  val leftArmContacts = LongContactTracker(PLAYERS_LEFT_ARM_USER_DATA)
  val rightArmContacts = LongContactTracker(PLAYERS_RIGHT_ARM_USER_DATA)

  val onGround: Boolean get() = !feetContacts.isEmpty
  val canMoveLeft: Boolean get() = onGround || leftArmContacts.isEmpty
  val canMoveRight: Boolean get() = onGround || rightArmContacts.isEmpty

  fun canMove(dir: Float): Boolean = when {
    dir < 0 -> canMoveLeft
    dir > 0 -> canMoveRight
    else -> true
  }

  fun clearContacts() {
    feetContacts.clear()
    leftArmContacts.clear()
    rightArmContacts.clear()
  }

  companion object : Mapper<GroundedComponent>() {

    var Entity.grounded by propertyFor(mapper)
    var Entity.groundedOrNull by optionalPropertyFor(mapper)
  }
}
