package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import no.elg.infiniteBootleg.util.WorldEntity
import no.elg.infiniteBootleg.util.breakBlocks
import no.elg.infiniteBootleg.util.inputMouseLocator
import no.elg.infiniteBootleg.util.interpolate
import no.elg.infiniteBootleg.util.placeBlocks
import no.elg.infiniteBootleg.world.ecs.api.restriction.ClientSystem
import no.elg.infiniteBootleg.world.ecs.components.InputEventQueueComponent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent.Companion.HotbarSlot
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent.Companion.hotbarComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.transients.CurrentlyBreakingComponent.Companion.currentlyBreakingComponentOrNull
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithInputEventFamily
import kotlin.math.sign

object InputSystem :
  EventSystem<InputEvent, InputEventQueueComponent>(controlledEntityWithInputEventFamily, InputEvent::class, InputEventQueueComponent.mapper),
  ClientSystem {

  override fun handleEvent(entity: Entity, deltaTime: Float, event: InputEvent) {
    val worldEntity = WorldEntity(entity.world, entity)
    when (event) {
      is InputEvent.KeyDownEvent -> worldEntity.keyDown(entity, event.keycode)
      is InputEvent.TouchDownEvent -> worldEntity.touchDown(event.button)
      is InputEvent.KeyTypedEvent -> Unit
      is InputEvent.KeyUpEvent -> Unit
      is InputEvent.MouseMovedEvent -> Unit
      is InputEvent.ScrolledEvent -> worldEntity.scrolled(event.amountY)
      is InputEvent.TouchDraggedEvent -> Unit
      is InputEvent.TouchUpEvent -> Unit
      is InputEvent.SpellCastEvent -> Unit
    }
  }

  private fun WorldEntity.touchDown(button: Int) {
    val update =
      when (button) {
        Input.Buttons.LEFT -> {
          entity.currentlyBreakingComponentOrNull?.reset()
          interpolate(true, ::breakBlocks)
        }

        Input.Buttons.RIGHT -> interpolate(true, ::placeBlocks)
        else -> false
      }

    if (update) {
      world.render.update()
    }
  }

  private fun WorldEntity.scrolled(amountY: Float) {
    val hotbarComponent = entity.hotbarComponentOrNull ?: return
    val direction = sign(amountY).toInt()

    val newOrdinal = (HotbarSlot.entries.size + hotbarComponent.selected.ordinal + direction) % HotbarSlot.entries.size
    hotbarComponent.selected = HotbarSlot.fromOrdinal(newOrdinal)
  }

  private fun WorldEntity.keyDown(entity: Entity, keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.T -> entity.teleport(inputMouseLocator.mouseWorldX, inputMouseLocator.mouseWorldY, killVelocity = true)
      Input.Keys.Q -> interpolate(true, ::placeBlocks)
    }

    val hotbarComponent = entity.hotbarComponentOrNull ?: return false
    val hotbarSlot = when (keycode) {
      Input.Keys.NUM_1, Input.Keys.NUMPAD_1 -> HotbarSlot.ONE
      Input.Keys.NUM_2, Input.Keys.NUMPAD_2 -> HotbarSlot.TWO
      Input.Keys.NUM_3, Input.Keys.NUMPAD_3 -> HotbarSlot.THREE
      Input.Keys.NUM_4, Input.Keys.NUMPAD_4 -> HotbarSlot.FOUR
      Input.Keys.NUM_5, Input.Keys.NUMPAD_5 -> HotbarSlot.FIVE
      Input.Keys.NUM_6, Input.Keys.NUMPAD_6 -> HotbarSlot.SIX
      Input.Keys.NUM_7, Input.Keys.NUMPAD_7 -> HotbarSlot.SEVEN
      Input.Keys.NUM_8, Input.Keys.NUMPAD_8 -> HotbarSlot.EIGHT
      Input.Keys.NUM_9, Input.Keys.NUMPAD_9 -> HotbarSlot.NINE
      else -> return false
    }
    hotbarComponent.selected = hotbarSlot
    return true
  }
}
