package no.elg.infiniteBootleg.world.box2d

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.groundedOrNull
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.system.event.PhysicsSystem.getOtherFixtureUserData

object OnGroundService {

  private fun handleTouchEvent(entity: Entity, event: PhysicsEvent, contacts: LongContactTracker, handle: LongContactTracker.(loc: Long) -> Unit) {
    val loc = event.getOtherFixtureUserData<Block>(entity, contacts.filter)?.compactWorldLoc ?: return
    contacts.handle(loc)
  }

  private fun handleBodyEvents(entity: Entity, event: PhysicsEvent, handle: LongContactTracker.(loc: Long) -> Unit) {
    val grounded = entity.groundedOrNull ?: return
    handleTouchEvent(entity, event, grounded.feetContacts, handle)
    handleTouchEvent(entity, event, grounded.leftArmContacts, handle)
    handleTouchEvent(entity, event, grounded.rightArmContacts, handle)
  }

  fun handleOnGroundContactBeginsEvent(entity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
    handleBodyEvents(entity, event, LongContactTracker::add)
  }

  fun handleOnGroundContactEndsEvent(entity: Entity, event: PhysicsEvent.ContactEndsEvent) {
    handleBodyEvents(entity, event, LongContactTracker::remove)
  }

  fun handleOnGroundBlockRemovedEvent(entity: Entity, event: PhysicsEvent.BlockRemovedEvent) {
    val grounded = entity.groundedOrNull ?: return
    val blockWorldLoc = event.compactLocation
    grounded.feetContacts.removeAll(blockWorldLoc)
    grounded.leftArmContacts.removeAll(blockWorldLoc)
    grounded.rightArmContacts.removeAll(blockWorldLoc)
  }
}
