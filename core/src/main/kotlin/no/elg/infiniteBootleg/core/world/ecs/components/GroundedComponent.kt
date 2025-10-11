package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.HorizontalDirection
import no.elg.infiniteBootleg.core.world.box2d.LongContactTracker
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.core.world.ecs.creation.HOLE_DETECTOR_USER_DATA
import no.elg.infiniteBootleg.core.world.ecs.creation.PLAYERS_EAST_ARM_USER_DATA
import no.elg.infiniteBootleg.core.world.ecs.creation.PLAYERS_FOOT_USER_DATA
import no.elg.infiniteBootleg.core.world.ecs.creation.PLAYERS_WEST_ARM_USER_DATA
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld

class GroundedComponent :
  EntitySavableComponent,
  AuthoritativeOnlyComponent {

  val feetContacts = LongContactTracker(PLAYERS_FOOT_USER_DATA)
  val holeContacts = LongContactTracker(HOLE_DETECTOR_USER_DATA)
  val westArmContacts = LongContactTracker(PLAYERS_WEST_ARM_USER_DATA)
  val eastArmContacts = LongContactTracker(PLAYERS_EAST_ARM_USER_DATA)

  val contacts = listOf(feetContacts, holeContacts, westArmContacts, eastArmContacts)

  val isStuckInHold: Boolean get() = holeContacts.size >= 2 && westArmContacts.isEmpty && eastArmContacts.isEmpty

  // To fix being stuck on in a 1x1 hole, allow jumping when both arms are in contact
  val onGround: Boolean get() = feetContacts.isNotEmpty
  val canJump: Boolean get() = feetContacts.isNotEmpty || isStuckInHold
  val canMoveWest: Boolean get() = westArmContacts.isEmpty
  val canMoveEast: Boolean get() = eastArmContacts.isEmpty

  fun canMove(dir: HorizontalDirection): Boolean =
    when (dir) {
      HorizontalDirection.WESTWARD -> canMoveWest
      HorizontalDirection.EASTWARD -> canMoveEast
      else -> false
    }

  fun clearContacts() {
    contacts.forEach { it.clear() }
  }

  operator fun contains(worldPos: WorldCompactLoc): Boolean {
    for (contact in contacts) {
      if (worldPos in contact) {
        return true
      }
    }
    return false
  }

  fun validate(entityPos: WorldCompactLoc, cutoffRadius: Double) {
    for (contact in contacts) {
      contact.validate(entityPos, cutoffRadius)
    }
  }

  override fun hudDebug(): String = "On Ground? $onGround, canJump? $canJump, canMoveWest? $canMoveWest, canMoveEast? $canMoveEast"
  
  companion object : EntityLoadableMapper<GroundedComponent>() {

    var Entity.groundedComponent by propertyFor(mapper)
    var Entity.groundedComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): GroundedComponent? = safeWith { GroundedComponent() }
    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasGrounded()
  }

  override fun EntityKt.Dsl.save() {
    grounded = ProtoWorld.Entity.Grounded.getDefaultInstance()
  }
}
