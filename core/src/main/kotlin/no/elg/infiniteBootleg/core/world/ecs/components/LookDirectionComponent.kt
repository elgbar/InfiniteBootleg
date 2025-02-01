package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.lookDirection
import no.elg.infiniteBootleg.protobuf.ProtoWorld

data class LookDirectionComponent(var direction: Direction = Direction.WEST) : EntitySavableComponent {

  override fun hudDebug(): String = direction.name

  companion object : EntityLoadableMapper<LookDirectionComponent>() {
    var Entity.lookDirectionComponentOrNull by optionalPropertyFor(mapper)

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasLookDirection()

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): LookDirectionComponent? {
      val vector = protoEntity.lookDirection.direction
      val direction = Direction.Companion.valueOf(vector)
      return safeWith { LookDirectionComponent(direction) }
    }
  }

  override fun EntityKt.Dsl.save() {
    lookDirection = lookDirection {
      direction = this@LookDirectionComponent.direction.toProtoVector2i()
    }
  }
}
