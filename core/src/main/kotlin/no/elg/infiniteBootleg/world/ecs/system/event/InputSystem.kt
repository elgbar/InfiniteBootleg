package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import no.elg.infiniteBootleg.util.WorldEntity
import no.elg.infiniteBootleg.util.breakBlocks
import no.elg.infiniteBootleg.util.inputMouseLocator
import no.elg.infiniteBootleg.util.interpolate
import no.elg.infiniteBootleg.util.placeBlocks
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.Tool
import no.elg.infiniteBootleg.world.ecs.components.InputEventQueueComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.transients.CurrentlyBreakingComponent.Companion.currentlyBreakingComponentOrNull
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithInputEventFamily

object InputSystem : EventSystem<InputEvent, InputEventQueueComponent>(controlledEntityWithInputEventFamily, InputEvent::class, InputEventQueueComponent.mapper) {

  override fun handleEvent(entity: Entity, deltaTime: Float, event: InputEvent) {
    val worldEntity = WorldEntity(entity.world, entity)
    when (event) {
      is InputEvent.KeyDownEvent -> worldEntity.keyDown(entity, event.keycode)
      is InputEvent.TouchDownEvent -> worldEntity.touchDown(event.button)
      is InputEvent.KeyTypedEvent -> Unit
      is InputEvent.KeyUpEvent -> Unit
      is InputEvent.MouseMovedEvent -> Unit
      is InputEvent.ScrolledEvent -> Unit
      is InputEvent.TouchDraggedEvent -> Unit
      is InputEvent.TouchUpEvent -> Unit
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

  private fun WorldEntity.keyDown(entity: Entity, keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.T -> entity.teleport(inputMouseLocator.mouseWorldX, inputMouseLocator.mouseWorldY)
      Input.Keys.Q -> interpolate(true, ::placeBlocks)
    }

    val selectedMaterial = entity.selectedInventoryItemComponentOrNull ?: return true

    val extra = if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) 10 else 0
    try {
      when (keycode) {
        Input.Keys.NUM_0, Input.Keys.NUMPAD_0 -> Material.entries[0 + extra]
        Input.Keys.NUM_1, Input.Keys.NUMPAD_1 -> Material.entries[1 + extra]
        Input.Keys.NUM_2, Input.Keys.NUMPAD_2 -> Material.entries[2 + extra]
        Input.Keys.NUM_3, Input.Keys.NUMPAD_3 -> Material.entries[3 + extra]
        Input.Keys.NUM_4, Input.Keys.NUMPAD_4 -> Material.entries[4 + extra]
        Input.Keys.NUM_5, Input.Keys.NUMPAD_5 -> Material.entries[5 + extra]
        Input.Keys.NUM_6, Input.Keys.NUMPAD_6 -> Material.entries[6 + extra]
        Input.Keys.NUM_7, Input.Keys.NUMPAD_7 -> Material.entries[7 + extra]
        Input.Keys.NUM_8, Input.Keys.NUMPAD_8 -> Material.entries[8 + extra]
        Input.Keys.NUM_9, Input.Keys.NUMPAD_9 -> Material.entries[9 + extra]
        else -> null
      }?.let {
        selectedMaterial.element = it
      }
    } catch (_: IndexOutOfBoundsException) {
      try {
        val materialSize = Material.entries.size
        when (keycode) {
          Input.Keys.NUM_0, Input.Keys.NUMPAD_0 -> Tool.entries[0 + extra - materialSize]
          Input.Keys.NUM_1, Input.Keys.NUMPAD_1 -> Tool.entries[1 + extra - materialSize]
          Input.Keys.NUM_2, Input.Keys.NUMPAD_2 -> Tool.entries[2 + extra - materialSize]
          Input.Keys.NUM_3, Input.Keys.NUMPAD_3 -> Tool.entries[3 + extra - materialSize]
          Input.Keys.NUM_4, Input.Keys.NUMPAD_4 -> Tool.entries[4 + extra - materialSize]
          Input.Keys.NUM_5, Input.Keys.NUMPAD_5 -> Tool.entries[5 + extra - materialSize]
          Input.Keys.NUM_6, Input.Keys.NUMPAD_6 -> Tool.entries[6 + extra - materialSize]
          Input.Keys.NUM_7, Input.Keys.NUMPAD_7 -> Tool.entries[7 + extra - materialSize]
          Input.Keys.NUM_8, Input.Keys.NUMPAD_8 -> Tool.entries[8 + extra - materialSize]
          Input.Keys.NUM_9, Input.Keys.NUMPAD_9 -> Tool.entries[9 + extra - materialSize]
          else -> null
        }?.let {
          selectedMaterial.element = it
        }
      } catch (_: IndexOutOfBoundsException) {
      }
    }
    return true
  }
}
