package no.elg.infiniteBootleg.world.box2d

import com.badlogic.ashley.core.Entity
import ktx.collections.GdxLongArray
import no.elg.infiniteBootleg.world.ecs.PLAYERS_FOOT_USER_DATA
import no.elg.infiniteBootleg.world.ecs.PLAYERS_LEFT_ARM_USER_DATA
import no.elg.infiniteBootleg.world.ecs.PLAYERS_RIGHT_ARM_USER_DATA
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.groundedOrNull
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.system.event.PhysicsSystem.getContactBlock

object OnGroundService {

  private val addHandle: (contacts: GdxLongArray, loc: Long) -> Unit = { contacts, loc -> contacts.add(loc) }
  private val removeHandle: (contacts: GdxLongArray, loc: Long) -> Unit = { contacts, loc -> contacts.removeValue(loc) }
  private val removeAllHandle: (contacts: GdxLongArray, loc: Long) -> Unit = { contacts, loc ->
    while (contacts.removeValue(loc)) {
      // Empty on purpose, removeValue has side effects
    }
  }

  private fun handleTouchEvent(entity: Entity, event: PhysicsEvent, contacts: GdxLongArray, userData: String, handle: (contacts: GdxLongArray, loc: Long) -> Unit) {
    val loc = event.getContactBlock(entity) { it == userData }?.compactWorldLoc ?: return
    handle(contacts, loc)
  }

  private fun handleBodyEvents(entity: Entity, event: PhysicsEvent, handle: (contacts: GdxLongArray, loc: Long) -> Unit) {
    val grounded = entity.groundedOrNull ?: return
    handleTouchEvent(entity, event, grounded.feetContacts, PLAYERS_FOOT_USER_DATA, handle)
    handleTouchEvent(entity, event, grounded.leftArmContacts, PLAYERS_LEFT_ARM_USER_DATA, handle)
    handleTouchEvent(entity, event, grounded.rightArmContacts, PLAYERS_RIGHT_ARM_USER_DATA, handle)
  }

  fun handleOnGroundContactBeginsEvent(entity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
    handleBodyEvents(entity, event, addHandle)
  }

  fun handleOnGroundContactEndsEvent(entity: Entity, event: PhysicsEvent.ContactEndsEvent) {
    handleBodyEvents(entity, event, removeHandle)
  }

  fun handleOnGroundBlockRemovedEvent(entity: Entity, event: PhysicsEvent.BlockRemovedEvent) {
    val grounded = entity.groundedOrNull ?: return
    val blockWorldLoc = event.compactLocation
    removeAllHandle(grounded.feetContacts, blockWorldLoc)
    removeAllHandle(grounded.leftArmContacts, blockWorldLoc)
    removeAllHandle(grounded.rightArmContacts, blockWorldLoc)
  }
}
