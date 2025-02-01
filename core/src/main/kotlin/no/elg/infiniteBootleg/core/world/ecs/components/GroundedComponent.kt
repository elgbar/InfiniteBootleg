package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.box2d.LongContactTracker
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.core.world.ecs.creation.HOLE_DETECTOR_USER_DATA
import no.elg.infiniteBootleg.core.world.ecs.creation.PLAYERS_FOOT_USER_DATA
import no.elg.infiniteBootleg.core.world.ecs.creation.PLAYERS_LEFT_ARM_USER_DATA
import no.elg.infiniteBootleg.core.world.ecs.creation.PLAYERS_RIGHT_ARM_USER_DATA
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld

class GroundedComponent : EntitySavableComponent, AuthoritativeOnlyComponent {

  val feetContacts = LongContactTracker(PLAYERS_FOOT_USER_DATA)
  val holeContacts = LongContactTracker(HOLE_DETECTOR_USER_DATA)
  val leftArmContacts = LongContactTracker(PLAYERS_LEFT_ARM_USER_DATA)
  val rightArmContacts = LongContactTracker(PLAYERS_RIGHT_ARM_USER_DATA)

  val contacts = listOf(feetContacts, holeContacts, leftArmContacts, rightArmContacts)

  // To fix being stuck on in a 1x1 hole, allow jumping when both arms are in contact
  val onGround: Boolean get() = feetContacts.isNotEmpty || (holeContacts.isNotEmpty && (leftArmContacts.isNotEmpty || rightArmContacts.isNotEmpty))
  val canMoveLeft: Boolean get() = onGround || leftArmContacts.isEmpty
  val canMoveRight: Boolean get() = onGround || rightArmContacts.isEmpty

  fun canMove(dir: Float): Boolean =
    when {
      dir < 0 -> canMoveLeft
      dir > 0 -> canMoveRight
      else -> true
    }

  fun clearContacts() {
    contacts.forEach { it.clear() }
  }

  override fun hudDebug(): String = "On Ground? $onGround, canMoveLeft? $canMoveLeft, canMoveRight? $canMoveRight"

  companion object : EntityLoadableMapper<GroundedComponent>() {

    var Entity.groundedComponent by propertyFor(mapper)
    var Entity.groundedComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): GroundedComponent? = safeWith { GroundedComponent() }
    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasGrounded()
    val PROTO_GROUNDED: ProtoWorld.Entity.Grounded = ProtoWorld.Entity.Grounded.getDefaultInstance()
  }

  override fun EntityKt.Dsl.save() {
    grounded = PROTO_GROUNDED
  }
}
