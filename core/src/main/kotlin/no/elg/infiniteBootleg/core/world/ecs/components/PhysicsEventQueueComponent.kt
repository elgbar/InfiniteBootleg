package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.with
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.components.events.ECSEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import org.jetbrains.annotations.Async

class PhysicsEventQueueComponent : ECSEventQueueComponent<PhysicsEvent>() {

  override fun EntityKt.Dsl.save() {
    physicsEvent = PROTO_PHYSICS_EVENT
  }

  companion object : EntityLoadableMapper<PhysicsEventQueueComponent>() {

    var Entity.physicsEventQueueOrNull by optionalPropertyFor(mapper)

    fun queuePhysicsEvent(@Async.Schedule event: PhysicsEvent) {
      if (event.isValid()) {
        if (event.entityA != null) {
          mapper.get(event.entityA)?.enqueue(event)
        }
        if (event.entityB != null) {
          mapper.get(event.entityB)?.enqueue(event)
        }
      }
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): PhysicsEventQueueComponent = with<PhysicsEventQueueComponent>()
    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasPhysicsEvent()
    val PROTO_PHYSICS_EVENT: ProtoWorld.Entity.PhysicsEvent = ProtoWorld.Entity.PhysicsEvent.getDefaultInstance()
  }
}
