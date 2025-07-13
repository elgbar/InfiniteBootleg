package no.elg.infiniteBootleg.core.world.box2d.service
//
//import com.badlogic.ashley.core.Entity
//import no.elg.infiniteBootleg.core.assets.InfAssets
//import no.elg.infiniteBootleg.core.world.box2d.ObjectContactTracker
//import no.elg.infiniteBootleg.core.world.ecs.components.DoorComponent
//import no.elg.infiniteBootleg.core.world.ecs.components.DoorComponent.Companion.doorComponentOrNull
//import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionNameComponent
//import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionNameComponent.Companion.textureRegionNameComponentOrNull
//import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
//import no.elg.infiniteBootleg.core.world.ecs.system.event.PhysicsSystem.getOtherFixtureUserData
//
//object DoorService {
//
//  private fun updateTexture(doorComponent: DoorComponent, textureComponent: TextureRegionNameComponent) {
//    if (doorComponent.closed) {
//      textureComponent.textureName = InfAssets.Companion.DOOR_CLOSED_TEXTURE
//    } else {
//      textureComponent.textureName = InfAssets.Companion.DOOR_OPEN_TEXTURE
//    }
//  }
//
//  private fun handleDoorContactEvent(entity: Entity, event: PhysicsEvent, handle: ObjectContactTracker<Entity>.(otherEntity: Entity) -> Unit) {
//    val textureComponent = entity.textureRegionNameComponentOrNull ?: return
//    val doorComponent = entity.doorComponentOrNull ?: return
//
//    val otherEntity = event.getOtherFixtureUserData<Entity>(entity) { it === entity } ?: return
//    doorComponent.contacts.handle(otherEntity)
//
//    updateTexture(doorComponent, textureComponent)
//  }
//
//  fun handleDoorContactBeginsEvent(entity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
//    handleDoorContactEvent(entity, event, ObjectContactTracker<Entity>::add)
//  }
//
//  fun handleDoorContactEndsEvent(entity: Entity, event: PhysicsEvent.ContactEndsEvent) {
//    handleDoorContactEvent(entity, event, ObjectContactTracker<Entity>::remove)
//  }
//}
