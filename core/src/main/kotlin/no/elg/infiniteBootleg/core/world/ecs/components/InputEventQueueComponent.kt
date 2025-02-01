package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.with
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.events.ECSEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.events.ECSEventQueueComponent.Companion.queueEvent
import no.elg.infiniteBootleg.core.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import java.util.concurrent.ConcurrentLinkedQueue

class InputEventQueueComponent : ECSEventQueueComponent<InputEvent>, AuthoritativeOnlyComponent {
  override val events = ConcurrentLinkedQueue<InputEvent>()

  override fun EntityKt.Dsl.save() {
    inputEvent = PROTO_INPUT_EVENT
  }

  companion object : EntityLoadableMapper<InputEventQueueComponent>() {
    var Entity.inputEventQueueOrNull by optionalPropertyFor(mapper)
    fun Engine.queueInputEvent(event: InputEvent, filter: (Entity) -> Boolean = { true }) {
      queueEvent(mapper, event, filter)
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): InputEventQueueComponent = with<InputEventQueueComponent>()
    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasInputEvent()
    val PROTO_INPUT_EVENT: ProtoWorld.Entity.InputEvent = ProtoWorld.Entity.InputEvent.getDefaultInstance()
  }
}
