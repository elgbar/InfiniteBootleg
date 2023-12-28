package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import ktx.ashley.allOf
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.util.inputMouseLocator
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
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.clientWorld
import no.elg.infiniteBootleg.world.ecs.components.transients.LastSpellCastComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.LastSpellCastComponent.Companion.lastSpellCastOrNull
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

  private val vector = Vector2()

  override fun processEntity(entity: Entity, deltaTime: Float) {
    if (ClientMain.inst().shouldIgnoreWorldInput()) {
      return
    }
    val world = entity.clientWorld ?: return
    val heldStaff = entity.selectedInventoryItemComponentOrNull?.element as? Staff ?: return
    val existingSpellState = entity.lastSpellCastOrNull
    if (existingSpellState.canCastAgain() && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
      // TODO Increase (distance/speed) by holding right click?

      val position = entity.position
      val newSpellState = heldStaff.createSpellState(entity)
      entity.add(LastSpellCastComponent(newSpellState))
      val velocityOrZero = entity.velocityOrZero

      inputMouseLocator.update(world)
      vector
        .set(inputMouseLocator.mouseWorldX, inputMouseLocator.mouseWorldY)
        .sub(position.x, position.y)
        .nor()

      world.engine.createSpellEntity(
        world,
        position.x,
        position.y,
        vector.x * newSpellState.spellVelocity.toFloat() + velocityOrZero.x,
        vector.y * newSpellState.spellVelocity.toFloat() + velocityOrZero.y,
        newSpellState
      ) {
        newSpellState.castMark = TimeSource.Monotonic.markNow() + newSpellState.castDelay
        newSpellState.staff.onSpellCast(newSpellState, it)
      }
    }
  }
}
