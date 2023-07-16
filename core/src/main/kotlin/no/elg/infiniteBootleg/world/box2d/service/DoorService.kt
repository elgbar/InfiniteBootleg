package no.elg.infiniteBootleg.world.box2d.service

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.world.box2d.ObjectContactTracker
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.components.transients.DoorComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.DoorComponent.Companion.doorOrNull
import no.elg.infiniteBootleg.world.ecs.components.transients.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.TextureRegionComponent.Companion.textureRegionOrNull
import no.elg.infiniteBootleg.world.ecs.system.event.PhysicsSystem.getOtherFixtureUserData

object DoorService {

  private fun updateTexture(doorComponent: DoorComponent, textureComponent: TextureRegionComponent) {
    if (doorComponent.closed) {
      textureComponent.texture = KAssets.doorClosedTexture
    } else {
      textureComponent.texture = KAssets.doorOpenTexture
    }
  }

  private fun handleDoorContactEvent(entity: Entity, event: PhysicsEvent, handle: ObjectContactTracker<Entity>.(otherEntity: Entity) -> Unit) {
    val textureComponent = entity.textureRegionOrNull ?: return
    val doorComponent = entity.doorOrNull ?: return

    val otherEntity = event.getOtherFixtureUserData<Entity>(entity) { it === entity } ?: return
    doorComponent.contacts.handle(otherEntity)

    updateTexture(doorComponent, textureComponent)
  }

  fun handleDoorContactBeginsEvent(entity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
    handleDoorContactEvent(entity, event, ObjectContactTracker<Entity>::add)
  }

  fun handleDoorContactEndsEvent(entity: Entity, event: PhysicsEvent.ContactEndsEvent) {
    handleDoorContactEvent(entity, event, ObjectContactTracker<Entity>::remove)
  }
}
