package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.box2d.FallingBlockContactService
import no.elg.infiniteBootleg.world.box2d.OnGroundService.handleOnGroundBlockRemovedEvent
import no.elg.infiniteBootleg.world.box2d.OnGroundService.handleOnGroundContactBeginsEvent
import no.elg.infiniteBootleg.world.box2d.OnGroundService.handleOnGroundContactEndsEvent
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithPhysicsEventFamily

object PhysicsSystem : EventSystem<PhysicsEvent, PhysicsEventQueue>(
  family = controlledEntityWithPhysicsEventFamily,
  priority = UPDATE_PRIORITY_DEFAULT,
  eventType = PhysicsEvent::class,
  queueMapper = PhysicsEventQueue.mapper
) {

  fun PhysicsEvent.getContactBlock(entity: Entity, filter: (userData: Any?) -> Boolean = { true }): Block? {
    val userDataA = fixtureA.userData
    val userDataB = fixtureB?.userData
    val otherUserData = when {
      filter(userDataA) && fixtureA.body.userData === entity -> userDataB
      filter(userDataB) && fixtureB?.body?.userData === entity -> userDataA
      else -> null
    } ?: return null
    return if (otherUserData is Block) {
      otherUserData
    } else {
      Main.logger().error("CheckOnGroundSystem", "Userdata of collided block is not an instance of Block, but rather: $this")
      null
    }
  }

  override fun handleEvent(entity: Entity, deltaTime: Float, event: PhysicsEvent) {
    when (event) {
      is PhysicsEvent.ContactBeginsEvent -> {
        handleOnGroundContactBeginsEvent(entity, event)
        FallingBlockContactService.handleFallingBlockContactBeginsEvent(entity, event)
      }

      is PhysicsEvent.ContactEndsEvent -> handleOnGroundContactEndsEvent(entity, event)
      is PhysicsEvent.BlockRemovedEvent -> handleOnGroundBlockRemovedEvent(entity, event)
      is PhysicsEvent.PostSolveContactEvent -> Unit
      is PhysicsEvent.PreSolveContactEvent -> Unit
    }
  }
}
