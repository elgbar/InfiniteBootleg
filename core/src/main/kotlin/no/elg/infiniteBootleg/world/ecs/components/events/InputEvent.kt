package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.with
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.StatelessAdditionalComponentsLoadableMapper
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEventQueue.Companion.queueEvent
import java.util.concurrent.ConcurrentLinkedQueue

class InputEventQueue : ECSEventQueue<InputEvent> {
  override val events = ConcurrentLinkedQueue<InputEvent>()

  override fun EntityKt.AdditionalComponentsKt.Dsl.save() {
    inputEvent = true
  }

  companion object : StatelessAdditionalComponentsLoadableMapper<InputEventQueue>() {
    var Entity.inputEventQueueOrNull by optionalPropertyFor(InputEventQueue.mapper)
    fun Engine.queueInputEvent(event: InputEvent, filter: (Entity) -> Boolean = { true }) {
      queueEvent(InputEventQueue.mapper, event, filter)
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.AdditionalComponents): InputEventQueue = with<InputEventQueue>()
    override fun ProtoWorld.Entity.AdditionalComponents.checkShouldLoad(): Boolean = hasInputEvent()
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
