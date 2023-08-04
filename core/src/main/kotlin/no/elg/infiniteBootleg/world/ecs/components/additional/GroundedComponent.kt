package no.elg.infiniteBootleg.world.ecs.components.additional

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.box2d.LongContactTracker
import no.elg.infiniteBootleg.world.ecs.api.AdditionalComponentsSavableComponent
import no.elg.infiniteBootleg.world.ecs.api.StatelessAdditionalComponentsLoadableMapper
import no.elg.infiniteBootleg.world.ecs.creation.PLAYERS_FOOT_USER_DATA
import no.elg.infiniteBootleg.world.ecs.creation.PLAYERS_LEFT_ARM_USER_DATA
import no.elg.infiniteBootleg.world.ecs.creation.PLAYERS_RIGHT_ARM_USER_DATA

class GroundedComponent : AdditionalComponentsSavableComponent {

  val feetContacts = LongContactTracker(PLAYERS_FOOT_USER_DATA)
  val leftArmContacts = LongContactTracker(PLAYERS_LEFT_ARM_USER_DATA)
  val rightArmContacts = LongContactTracker(PLAYERS_RIGHT_ARM_USER_DATA)

  val onGround: Boolean get() = !feetContacts.isEmpty
  val canMoveLeft: Boolean get() = onGround || leftArmContacts.isEmpty
  val canMoveRight: Boolean get() = onGround || rightArmContacts.isEmpty

  fun canMove(dir: Float): Boolean = when {
    dir < 0 -> canMoveLeft
    dir > 0 -> canMoveRight
    else -> true
  }

  fun clearContacts() {
    feetContacts.clear()
    leftArmContacts.clear()
    rightArmContacts.clear()
  }

  companion object : StatelessAdditionalComponentsLoadableMapper<GroundedComponent>() {

    var Entity.groundedComponent by propertyFor(mapper)
    var Entity.groundedComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.AdditionalComponents): GroundedComponent = with(GroundedComponent())
    override fun ProtoWorld.Entity.AdditionalComponents.checkShouldLoad(): Boolean = hasGrounded()
  }

  override fun EntityKt.AdditionalComponentsKt.Dsl.save() {
    grounded = true
  }
}