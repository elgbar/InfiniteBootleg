package no.elg.infiniteBootleg.world.ecs.api

import com.badlogic.ashley.core.Component
import ktx.ashley.EngineEntity
import ktx.ashley.Mapper

abstract class ParentLoadableMapper<T : Component, P> : Mapper<T>() {

  fun load(engineEntity: EngineEntity, protoEntity: P): T? {
    if (protoEntity.checkShouldLoad()) {
      return engineEntity.loadInternal(protoEntity)
    }
    return null
  }

  protected abstract fun EngineEntity.loadInternal(protoEntity: P): T

  protected abstract fun P.checkShouldLoad(): Boolean
}
