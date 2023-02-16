package no.elg.infiniteBootleg.world.ecs.system.event

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlled
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEventQueue
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithInputEventFamily

object InputSystem : EventSystem<InputEvent, InputEventQueue>(controlledEntityWithInputEventFamily, UPDATE_PRIORITY_DEFAULT, InputEvent::class, InputEventQueue.mapper) {

  override fun handleEvent(entity: Entity, deltaTime: Float, event: InputEvent) {
    val controls = entity.locallyControlled.keyboardControls
    when (event) {
      is InputEvent.KeyDownEvent -> controls.keyDown(entity, event.keycode)
      is InputEvent.TouchDownEvent -> controls.touchDown(entity, event.button)
      is InputEvent.KeyTypedEvent -> Unit
      is InputEvent.KeyUpEvent -> Unit
      is InputEvent.MouseMovedEvent -> Unit
      is InputEvent.ScrolledEvent -> Unit
      is InputEvent.TouchDraggedEvent -> Unit
      is InputEvent.TouchUpEvent -> Unit
    }
  }
}
