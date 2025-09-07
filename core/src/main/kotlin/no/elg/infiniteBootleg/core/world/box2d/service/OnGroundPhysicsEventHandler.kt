package no.elg.infiniteBootleg.core.world.box2d.service

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.core.world.box2d.LongContactTracker
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent.Companion.groundedComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.system.event.PhysicsSystem
import org.jetbrains.annotations.Async

object OnGroundPhysicsEventHandler : PhysicsSystem.PhysicsEventHandler {

  private fun handleTouchEvent(entity: Entity, event: PhysicsEvent, contacts: LongContactTracker, handle: LongContactTracker.(loc: Long) -> Unit) {
    if (contacts.filter(event.getThisUserDataForShape(entity))) {
      val otherEntity = event.getOtherEventBlock(entity) ?: return
      val loc = otherEntity.compactWorldLoc
      contacts.handle(loc)
    }
  }

  private fun handleBodyEvents(entity: Entity, event: PhysicsEvent, handle: LongContactTracker.(loc: Long) -> Unit) {
    val grounded = entity.groundedComponentOrNull ?: return
    for (contact in grounded.contacts) {
      handleTouchEvent(entity, event, contact, handle)
    }
  }

  fun handleOnGroundBlockRemovedEvent(entity: Entity, event: PhysicsEvent.BlockRemovedEvent) {
    val grounded = entity.groundedComponentOrNull ?: return
    val blockWorldLoc = event.compactLocation
    for (contact in grounded.contacts) {
      contact.remove(blockWorldLoc)
    }
  }

  override fun handleEvent(entity: Entity, @Async.Execute event: PhysicsEvent) {
    when (event) {
      is PhysicsEvent.ContactBeginsEvent -> handleBodyEvents(entity, event, LongContactTracker::add)
      is PhysicsEvent.ContactEndsEvent -> handleBodyEvents(entity, event, LongContactTracker::remove)
      is PhysicsEvent.BlockRemovedEvent -> handleOnGroundBlockRemovedEvent(entity, event)
      else -> Unit
    }
  }
}
