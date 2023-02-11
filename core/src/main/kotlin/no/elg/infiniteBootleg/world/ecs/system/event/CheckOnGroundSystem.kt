package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import ktx.collections.GdxLongArray
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.PLAYERS_FOOT_USER_DATA
import no.elg.infiniteBootleg.world.ecs.PLAYERS_LEFT_ARM_USER_DATA
import no.elg.infiniteBootleg.world.ecs.PLAYERS_RIGHT_ARM_USER_DATA
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.grounded
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithPhysicsEventFamily

object CheckOnGroundSystem :
  EventSystem<PhysicsEvent, PhysicsEventQueue>(controlledEntityWithPhysicsEventFamily, UPDATE_PRIORITY_DEFAULT, PhysicsEvent::class, PhysicsEventQueue.mapper) {

  private fun PhysicsEvent.getPlayerSpecialFixture(entity: Entity, userData: Any): Any? {
    val userDataA = fixtureA.userData
    val userDataB = fixtureB?.userData
    return when {
      userDataA == userData && fixtureA.body.userData === entity -> userDataB
      userDataB == userData && fixtureB?.body?.userData === entity -> userDataA
      else -> null
    }
  }

  private val Any?.blockWorldLoc: Long?
    get() {
      return if (this is Block) {
        compactWorldLoc
      } else {
        Main.logger().error("CheckOnGroundSystem", "Userdata of collided block is not an instance of Block, but rather: $this")
        null
      }
    }

  private val addHandle: (contacts: GdxLongArray, loc: Long) -> Unit = { contacts, loc -> contacts.add(loc) }
  private val removeHandle: (contacts: GdxLongArray, loc: Long) -> Unit = { contacts, loc -> contacts.removeValue(loc) }
  private val removeAllHandle: (contacts: GdxLongArray, loc: Long) -> Unit = { contacts, loc ->
    while (contacts.removeValue(loc)) {
      // Empty on purpose, removeValue has side effects
    }
  }

  private fun handleTouchEvent(entity: Entity, event: PhysicsEvent, contacts: GdxLongArray, userData: String, handle: (contacts: GdxLongArray, loc: Long) -> Unit) {
    val loc = event.getPlayerSpecialFixture(entity, userData)?.blockWorldLoc ?: return
    handle(contacts, loc)
  }

  private fun handleBodyEvents(entity: Entity, event: PhysicsEvent, handle: (contacts: GdxLongArray, loc: Long) -> Unit) {
    handleTouchEvent(entity, event, entity.grounded.feetContacts, PLAYERS_FOOT_USER_DATA, handle)
    handleTouchEvent(entity, event, entity.grounded.leftArmContacts, PLAYERS_LEFT_ARM_USER_DATA, handle)
    handleTouchEvent(entity, event, entity.grounded.rightArmContacts, PLAYERS_RIGHT_ARM_USER_DATA, handle)
  }

  override fun handleEvent(entity: Entity, deltaTime: Float, event: PhysicsEvent) {
    when (event) {
      is PhysicsEvent.ContactBeginsEvent -> handleBodyEvents(entity, event, addHandle)
      is PhysicsEvent.ContactEndsEvent -> handleBodyEvents(entity, event, removeHandle)
      is PhysicsEvent.BlockRemovedEvent -> {
        val blockWorldLoc = event.compactLocation
        removeAllHandle(entity.grounded.feetContacts, blockWorldLoc)
        removeAllHandle(entity.grounded.leftArmContacts, blockWorldLoc)
        removeAllHandle(entity.grounded.rightArmContacts, blockWorldLoc)
      }

      is PhysicsEvent.PostSolveContactEvent -> Unit
      is PhysicsEvent.PreSolveContactEvent -> Unit
    }
  }
}
