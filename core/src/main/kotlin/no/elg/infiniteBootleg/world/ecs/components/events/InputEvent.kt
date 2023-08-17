package no.elg.infiniteBootleg.world.ecs.components.events

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
