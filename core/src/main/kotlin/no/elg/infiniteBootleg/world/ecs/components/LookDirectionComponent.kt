package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.lookDirection
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.vector2i
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent

data class LookDirectionComponent(var direction: Direction = Direction.WEST) : EntitySavableComponent {
  companion object : EntityLoadableMapper<LookDirectionComponent>() {
    var Entity.lookDirectionComponentOrNull by optionalPropertyFor(LookDirectionComponent.mapper)

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasLookDirection()

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): LookDirectionComponent {
      val vector = protoEntity.lookDirection.direction
      val direction = Direction.valueOf(vector.x, vector.y)
      return with(LookDirectionComponent(direction))
    }
  }

  override fun EntityKt.Dsl.save() {
    lookDirection {
      direction = vector2i {
        this.x = this@LookDirectionComponent.direction.dx
        this.y = this@LookDirectionComponent.direction.dy
      }
    }
  }
}
