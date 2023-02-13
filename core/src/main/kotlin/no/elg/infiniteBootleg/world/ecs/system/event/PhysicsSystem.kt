package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.box2d.DoorService
import no.elg.infiniteBootleg.world.box2d.FallingBlockContactService
import no.elg.infiniteBootleg.world.box2d.OnGroundService
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.entityWithPhysicsEventFamily

object PhysicsSystem : EventSystem<PhysicsEvent, PhysicsEventQueue>(
  family = entityWithPhysicsEventFamily,
  priority = UPDATE_PRIORITY_DEFAULT,
  eventType = PhysicsEvent::class,
  queueMapper = PhysicsEventQueue.mapper
) {

  inline fun <reified T : Any> PhysicsEvent.getOtherFixtureUserData(entity: Entity, filter: (userData: Any?) -> Boolean): T? {
    val userDataA = fixtureA.userData
    val userDataB = fixtureB?.userData
    val otherUserData = when {
      filter(userDataA) && fixtureA.body.userData === entity -> userDataB
      filter(userDataB) && fixtureB?.body?.userData === entity -> userDataA
      else -> null
    } ?: return null
    return if (otherUserData is T) {
      Main.logger()
      otherUserData
    } else {
      null
    }
  }

  override fun handleEvent(entity: Entity, deltaTime: Float, event: PhysicsEvent) {
    when (event) {
      is PhysicsEvent.ContactBeginsEvent -> {
        OnGroundService.handleOnGroundContactBeginsEvent(entity, event)
        FallingBlockContactService.handleFallingBlockContactBeginsEvent(entity, event)
        DoorService.handleDoorContactBeginsEvent(entity, event)
      }

      is PhysicsEvent.ContactEndsEvent -> {
        OnGroundService.handleOnGroundContactEndsEvent(entity, event)
        DoorService.handleDoorContactEndsEvent(entity, event)
      }

      is PhysicsEvent.BlockRemovedEvent -> OnGroundService.handleOnGroundBlockRemovedEvent(entity, event)
      is PhysicsEvent.PostSolveContactEvent -> Unit
      is PhysicsEvent.PreSolveContactEvent -> Unit
    }
  }
}
