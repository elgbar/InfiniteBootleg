package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.util.getPlayerStandingOn
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.grounded
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithPhysicsEventFamily

object CheckOnGroundSystem : EventSystem<PhysicsEvent>(controlledEntityWithPhysicsEventFamily, UPDATE_PRIORITY_DEFAULT, PhysicsEvent::class) {

  override fun handleEvent(entity: Entity, deltaTime: Float) {
    handleInputEvent(entity, PhysicsEvent.ContactBeginsEvent.mapper) {
      println("handling contact begins event")
      val playersFoot = it.contact.getPlayerStandingOn(entity) ?: return@handleInputEvent
      entity.grounded += playersFoot
    }
    handleInputEvent(entity, PhysicsEvent.ContactEndsEvent.mapper) {
      println("handling contact ends event")
      val playersFoot = it.contact.getPlayerStandingOn(entity) ?: return@handleInputEvent
      entity.grounded -= playersFoot
    }
  }
}
