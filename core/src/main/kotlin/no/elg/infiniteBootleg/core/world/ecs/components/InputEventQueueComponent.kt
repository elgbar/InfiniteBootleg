package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.with
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.events.ECSEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld

class InputEventQueueComponent :
  ECSEventQueueComponent<InputEvent>(),
  AuthoritativeOnlyComponent {

  override fun EntityKt.Dsl.save() {
    inputEvent = PROTO_INPUT_EVENT
  }

  companion object : EntityLoadableMapper<InputEventQueueComponent>() {
    var Entity.inputEventQueueOrNull by optionalPropertyFor(mapper)
    fun World.queueInputEventAsync(event: InputEvent, filter: (Entity) -> Boolean = ALLOW_ALL_FILTER) {
      queueEventAsync(mapper, event, filter)
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): InputEventQueueComponent = with<InputEventQueueComponent>()
    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasInputEvent()
    val PROTO_INPUT_EVENT: ProtoWorld.Entity.InputEvent = ProtoWorld.Entity.InputEvent.getDefaultInstance()
  }
}
