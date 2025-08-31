package no.elg.infiniteBootleg.client.world.box2d.service

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.client.world.ecs.components.transients.RotatableTextureRegionComponent.Companion.updateTexture
import no.elg.infiniteBootleg.core.assets.InfAssets
import no.elg.infiniteBootleg.core.world.box2d.ObjectContactTracker
import no.elg.infiniteBootleg.core.world.ecs.components.DoorComponent.Companion.doorComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.system.event.PhysicsSystem

object DoorService : PhysicsSystem.PhysicsEventHandler {

  private fun handleDoorContactEvent(doorEntity: Entity, event: PhysicsEvent, handle: ObjectContactTracker<Entity>.(otherEntity: Entity) -> Unit) {
    val otherEntity = event.getOtherEventEntity(doorEntity) ?: return
    val doorComponent = doorEntity.doorComponentOrNull ?: return
    doorComponent.contacts.handle(otherEntity)

    if (doorComponent.closed) {
      doorEntity.updateTexture(InfAssets.DOOR_CLOSED_TEXTURE)
    } else if (!doorComponent.closed) {
      doorEntity.updateTexture(InfAssets.DOOR_OPEN_TEXTURE)
    }
  }

  override fun handleEvent(entity: Entity, event: PhysicsEvent) {
    when (event) {
      is PhysicsEvent.ContactBeginsEvent -> handleDoorContactEvent(entity, event, ObjectContactTracker<Entity>::add)
      is PhysicsEvent.ContactEndsEvent -> handleDoorContactEvent(entity, event, ObjectContactTracker<Entity>::remove)
      else -> {
      }
    }
  }
}
