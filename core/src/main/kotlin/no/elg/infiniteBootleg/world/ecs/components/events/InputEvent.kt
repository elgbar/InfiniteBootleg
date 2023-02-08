package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import java.util.concurrent.ConcurrentLinkedQueue

class InputEventQueue : ECSEventQueue<InputEvent> {
  override val events = ConcurrentLinkedQueue<InputEvent>()
  
  companion object : Mapper<InputEventQueue>() {
    var Entity.inputEventQueueOrNull by optionalPropertyFor(InputEventQueue.mapper)
  }
}

sealed interface InputEvent : ECSEvent {

  data class KeyDownEvent(val keycode: Int) : InputEvent

  data class KeyUpEvent(val keycode: Int) : InputEvent

  data class KeyTypedEvent(val char: Char) : InputEvent

  data class TouchDownEvent(val screenX: Int, val screenY: Int, val pointer: Int, val button: Int) : InputEvent

  data class TouchUpEvent(val screenX: Int, val screenY: Int, val pointer: Int, val button: Int) : InputEvent

  data class TouchDraggedEvent(val screenX: Int, val screenY: Int, val pointer: Int) : InputEvent

  data class MouseMovedEvent(val screenX: Int, val screenY: Int) : InputEvent

  data class ScrolledEvent(val amountX: Float, val amountY: Float) : InputEvent
}
