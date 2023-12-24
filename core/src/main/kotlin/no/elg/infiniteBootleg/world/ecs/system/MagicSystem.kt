package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import ktx.ashley.allOf
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.world.Staff
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.api.restriction.ClientSystem
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityOrZero
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.transients.SpellStateComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.SpellStateComponent.Companion.spellStateOrNull
import no.elg.infiniteBootleg.world.ecs.creation.createSpellEntity
import no.elg.infiniteBootleg.world.magic.SpellState.Companion.canCastAgain
import kotlin.time.TimeSource

object MagicSystem :
  IteratingSystem(
    allOf(
      WorldComponent::class,
      LocallyControlledComponent::class,
      SelectedInventoryItemComponent::class,
      PositionComponent::class
    ).get(),
    UPDATE_PRIORITY_DEFAULT
  ),
  ClientSystem {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    if (ClientMain.inst().shouldIgnoreWorldInput()) {
      return
    }
    val heldStaff = entity.selectedInventoryItemComponentOrNull?.element as? Staff ?: return
    val existingSpellState = entity.spellStateOrNull
    if (existingSpellState.canCastAgain() && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
      // TODO Increase (distance/speed) by holding right click?

      val newSpellState = heldStaff.createSpellState(entity).also { entity.add(SpellStateComponent(it)) }
      val world = entity.world
      val position = entity.position
      val velocityOrZero = entity.velocityOrZero
      world.engine.createSpellEntity(
        world,
        position.x,
        position.y,
        velocityOrZero.x * 5 + 10,
        velocityOrZero.y * 5 + 5,
        newSpellState
      ) {
        newSpellState.castMark = TimeSource.Monotonic.markNow() + newSpellState.castDelay
        newSpellState.staff.castSpell(newSpellState, it)
      }
    }
  }
}
