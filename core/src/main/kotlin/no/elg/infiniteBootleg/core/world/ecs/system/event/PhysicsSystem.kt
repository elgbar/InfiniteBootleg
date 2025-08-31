package no.elg.infiniteBootleg.core.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.world.box2d.service.AuthoritativeFallingBlockPhysicsEventHandler
import no.elg.infiniteBootleg.core.world.box2d.service.OnGroundPhysicsEventHandler
import no.elg.infiniteBootleg.core.world.box2d.service.SpellContactPhysicsEventHandler
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.core.world.ecs.system.api.EventSystem

class PhysicsSystem :
  EventSystem<PhysicsEvent, PhysicsEventQueueComponent>(
    family = entityWithPhysicsEventFamily,
    eventType = PhysicsEvent::class,
    queueMapper = PhysicsEventQueueComponent.mapper
  ) {

  val handlers = mutableListOf<PhysicsEventHandler>().apply {
    add(SpellContactPhysicsEventHandler)
    if (Main.isAuthoritative) {
      add(AuthoritativeFallingBlockPhysicsEventHandler)
    }
  }

  override fun condition(entity: Entity): Boolean = Main.inst().isAuthorizedToChange(entity)

  override fun handleEvent(entity: Entity, event: PhysicsEvent) {
    handlers.forEach { it.handleEvent(entity, event) }
  }

  interface PhysicsEventHandler {
    fun handleEvent(entity: Entity, event: PhysicsEvent)
  }
}
