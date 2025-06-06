package no.elg.infiniteBootleg.core.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.world.box2d.service.DoorService
import no.elg.infiniteBootleg.core.world.box2d.service.FallingBlockContactService
import no.elg.infiniteBootleg.core.world.box2d.service.OnGroundService
import no.elg.infiniteBootleg.core.world.box2d.service.SpellContactService
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.core.world.ecs.system.api.EventSystem

object PhysicsSystem : EventSystem<PhysicsEvent, PhysicsEventQueueComponent>(
  family = entityWithPhysicsEventFamily,
  eventType = PhysicsEvent::class,
  queueMapper = PhysicsEventQueueComponent.Companion.mapper
) {

  inline fun <reified T : Any> PhysicsEvent.getOtherFixtureUserData(entity: Entity, filter: (userData: Any?) -> Boolean): T? {
    val userDataA: Any? = fixtureA?.userData
    val userDataB: Any? = fixtureB?.userData
    val otherUserData: Any? = when {
      filter(userDataA) && fixtureA?.body?.userData === entity -> userDataB
      filter(userDataB) && fixtureB?.body?.userData === entity -> userDataA
      else -> null
    }
    return otherUserData as? T
  }

  override fun condition(entity: Entity): Boolean = Main.inst().isAuthorizedToChange(entity)

  override fun handleEvent(entity: Entity, event: PhysicsEvent) {
    when (event) {
      is PhysicsEvent.ContactBeginsEvent -> {
        OnGroundService.handleOnGroundContactBeginsEvent(entity, event)
        FallingBlockContactService.handleFallingBlockContactBeginsEvent(entity, event)
        DoorService.handleDoorContactBeginsEvent(entity, event)
        SpellContactService.handleSpellContactBeginsEvent(entity, event)
      }

      is PhysicsEvent.ContactEndsEvent -> {
        DoorService.handleDoorContactEndsEvent(entity, event)
      }

      is PhysicsEvent.BlockRemovedEvent -> OnGroundService.handleOnGroundBlockRemovedEvent(entity, event)
      is PhysicsEvent.PostSolveContactEvent -> Unit
      is PhysicsEvent.PreSolveContactEvent -> Unit
    }
  }
}
