package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEventQueue.Companion.queueEvent
import java.util.concurrent.ConcurrentLinkedQueue

class InputEventQueue : ECSEventQueue<InputEvent> {
  override val events = ConcurrentLinkedQueue<InputEvent>()

  companion object : Mapper<InputEventQueue>() {
    var Entity.inputEventQueueOrNull by optionalPropertyFor(InputEventQueue.mapper)
    inline fun Engine.queueInputEvent(event: InputEvent, filter: (Entity) -> Boolean = { true }) {
      queueEvent(InputEventQueue.mapper, event, filter)
    }
  }
}

sealed interface InputEvent : ECSEvent {

  interface MouseInputEvent {
    val screenX: Int
    val screenY: Int
  }

  data class KeyDownEvent(val keycode: Int) : InputEvent

  data class KeyUpEvent(val keycode: Int) : InputEvent

  data class KeyTypedEvent(val char: Char) : InputEvent

  data class TouchDownEvent(override val screenX: Int, override val screenY: Int, val pointer: Int, val button: Int) : InputEvent, MouseInputEvent

  data class TouchUpEvent(override val screenX: Int, override val screenY: Int, val pointer: Int, val button: Int) : InputEvent, MouseInputEvent

  data class TouchDraggedEvent(override val screenX: Int, override val screenY: Int, val pointer: Int) : InputEvent, MouseInputEvent

  data class MouseMovedEvent(override val screenX: Int, override val screenY: Int) : InputEvent, MouseInputEvent

  data class ScrolledEvent(val amountX: Float, val amountY: Float) : InputEvent
}
