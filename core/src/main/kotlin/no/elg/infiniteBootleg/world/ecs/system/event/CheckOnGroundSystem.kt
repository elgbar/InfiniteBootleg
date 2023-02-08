package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.Fixture
import ktx.collections.GdxLongArray
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.PLAYERS_FOOT_USER_DATA
import no.elg.infiniteBootleg.world.ecs.PLAYERS_LEFT_ARM_USER_DATA
import no.elg.infiniteBootleg.world.ecs.PLAYERS_RIGHT_ARM_USER_DATA
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.grounded
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithPhysicsEventFamily

object CheckOnGroundSystem :
  EventSystem<PhysicsEvent, PhysicsEventQueue>(controlledEntityWithPhysicsEventFamily, UPDATE_PRIORITY_DEFAULT, PhysicsEvent::class, PhysicsEventQueue.mapper) {

  private fun PhysicsEvent.getPlayerSpecialFixture(entity: Entity, userData: Any): Fixture? {
    return when {
      fixtureA.userData == userData && fixtureA.body.userData === entity -> fixtureB
      fixtureB.userData == userData && fixtureB.body.userData === entity -> fixtureA
      else -> null
    }
  }

  private fun Fixture.getBlockWorldLoc(): Long? {
    val userData = userData
    return if (userData is Block) {
      userData.compactWorldLoc
    } else {
      Main.logger().error("CheckOnGroundSystem", "Userdata of collided block is not an instance of Block, but rather: $userData")
      null
    }
  }

  private fun handleTouchEvent(fixture: Fixture, contacts: GdxLongArray, add: Boolean) {
    val loc = fixture.getBlockWorldLoc() ?: return
    if (add) {
      contacts.add(loc)
    } else {
      contacts.removeValue(loc)
    }
  }

  private fun handleFeetEvent(entity: Entity, event: PhysicsEvent, add: Boolean) {
    val fixture = event.getPlayerSpecialFixture(entity, PLAYERS_FOOT_USER_DATA) ?: return
    handleTouchEvent(fixture, entity.grounded.feetContacts, add)
  }

  private fun handleLeftArmEvent(entity: Entity, event: PhysicsEvent, add: Boolean) {
    val fixture = event.getPlayerSpecialFixture(entity, PLAYERS_LEFT_ARM_USER_DATA) ?: return
    handleTouchEvent(fixture, entity.grounded.leftArmContacts, add)
  }

  private fun handleRightArmEvent(entity: Entity, event: PhysicsEvent, add: Boolean) {
    val fixture = event.getPlayerSpecialFixture(entity, PLAYERS_RIGHT_ARM_USER_DATA) ?: return
    handleTouchEvent(fixture, entity.grounded.rightArmContacts, add)
  }

  override fun handleEvent(entity: Entity, deltaTime: Float, event: PhysicsEvent) {
    when (event) {
      is PhysicsEvent.ContactBeginsEvent -> {
        handleFeetEvent(entity, event, true)
        handleLeftArmEvent(entity, event, true)
        handleRightArmEvent(entity, event, true)
      }

      is PhysicsEvent.ContactEndsEvent -> {
        handleFeetEvent(entity, event, false)
        handleLeftArmEvent(entity, event, false)
        handleRightArmEvent(entity, event, false)
      }

      is PhysicsEvent.PostSolveContactEvent -> Unit
      is PhysicsEvent.PreSolveContactEvent -> Unit
    }
  }
}
