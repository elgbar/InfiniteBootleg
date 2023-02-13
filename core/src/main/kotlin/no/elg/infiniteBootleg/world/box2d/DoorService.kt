package no.elg.infiniteBootleg.world.box2d

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.world.ecs.components.DoorComponent
import no.elg.infiniteBootleg.world.ecs.components.DoorComponent.Companion.doorOrNull
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent.Companion.textureRegionOrNull
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
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
