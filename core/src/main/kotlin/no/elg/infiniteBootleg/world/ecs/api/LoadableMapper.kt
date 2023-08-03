package no.elg.infiniteBootleg.world.ecs.api

import com.badlogic.ashley.core.Component
import com.google.protobuf.Message
import ktx.ashley.EngineEntity
import ktx.ashley.Mapper
import no.elg.infiniteBootleg.protobuf.ProtoWorld

abstract class LoadableMapper<C : Component, P : Message, S> : Mapper<C>() {
  fun load(engineEntity: EngineEntity, protoEntity: P, state: () -> S): C? {
    if (protoEntity.checkShouldLoad()) {
      return engineEntity.loadInternal(protoEntity, state())
    }
    return null
  }

  protected abstract fun EngineEntity.loadInternal(protoEntity: P, state: S): C

  protected abstract fun P.checkShouldLoad(): Boolean
}

abstract class StatelessLoadableMapper<C : Component, P : Message> : LoadableMapper<C, P, Unit>() {

  fun load(engineEntity: EngineEntity, protoEntity: P): C? = load(engineEntity, protoEntity) { }

  @Deprecated("Do not use stateful loading in this class", ReplaceWith("loadInternal(protoEntity)"))
  final override fun EngineEntity.loadInternal(protoEntity: P, state: Unit): C = loadInternal(protoEntity)

  protected abstract fun EngineEntity.loadInternal(protoEntity: P): C
}

abstract class EntityLoadableMapper<T : Component> : StatelessLoadableMapper<T, ProtoWorld.Entity>()
abstract class TagLoadableMapper<T : Component> : StatelessLoadableMapper<T, ProtoWorld.Entity.Tags>()
abstract class StatelessAdditionalComponentsLoadableMapper<T : Component> : StatelessLoadableMapper<T, ProtoWorld.Entity.AdditionalComponents>()
abstract class AdditionalComponentsLoadableMapper<T : Component, S> : LoadableMapper<T, ProtoWorld.Entity.AdditionalComponents, S>()
