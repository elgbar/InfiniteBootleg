package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.Fixture
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.PLAYERS_FOOT_USER_DATA
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.grounded
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithPhysicsEventFamily

object CheckOnGroundSystem :
  EventSystem<PhysicsEvent, PhysicsEventQueue>(controlledEntityWithPhysicsEventFamily, UPDATE_PRIORITY_DEFAULT, PhysicsEvent::class, PhysicsEventQueue.mapper) {

  private fun PhysicsEvent.getPlayerStandingOn(entity: Entity): Fixture? {
    return when {
      fixtureA.userData == PLAYERS_FOOT_USER_DATA && fixtureA.body.userData === entity -> fixtureB
      fixtureB.userData == PLAYERS_FOOT_USER_DATA && fixtureB.body.userData === entity -> fixtureA
      else -> null
    }
  }

  override fun handleEvent(entity: Entity, deltaTime: Float, event: PhysicsEvent) {
    println("handling physics event $event")
    when (event) {
      is PhysicsEvent.ContactBeginsEvent -> {
        val groundStoodOn = event.getPlayerStandingOn(entity) ?: return
        println("handling contact begins event")
        val userData = groundStoodOn.userData
        if (userData is Block) {
          entity.grounded += userData.compactWorldLoc
        } else {
          Main.logger().error("CheckOnGroundSystem", "Userdata of collided block is not an instance of Block, but rather: $userData")
        }
      }

      is PhysicsEvent.ContactEndsEvent -> {
        val groundStoodOn = event.getPlayerStandingOn(entity) ?: return
        println("handling contact ends event")

        val userData = groundStoodOn.userData
        if (userData is Block) {
          entity.grounded -= userData.compactWorldLoc
        }
      }

      is PhysicsEvent.PostSolveContactEvent -> Unit
      is PhysicsEvent.PreSolveContactEvent -> Unit
    }
  }
}
