package no.elg.infiniteBootleg.world.ecs.components.required

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import ktx.ashley.EngineEntity
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.vector2f
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.worldToBlock
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.ParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.components.transients.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.world.ecs.with
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f as ProtoVector2f

data class PositionComponent(var x: Float, var y: Float) : EntitySavableComponent {

  val blockX: Int get() = worldToBlock(x)
  val blockY: Int get() = worldToBlock(y)

  fun toProtoVector2f(): ProtoVector2f = vector2f {
    x = this@PositionComponent.x
    y = this@PositionComponent.y
  }

  fun toVector2(): Vector2 = Vector2(x, y)

  override fun EntityKt.Dsl.save() {
    position = vector2f {
      x = this@PositionComponent.x
      y = this@PositionComponent.y
    }
  }

  companion object : ParentLoadableMapper<PositionComponent, ProtoWorld.Entity>() {
    val Entity.position: Vector2 get() = positionComponent.toVector2()
    val Entity.compactBlockLoc: Long get() = positionComponent.run { compactLoc(blockX, blockY) }
    val Entity.compactChunkLoc: Long get() = positionComponent.run { compactLoc(x.worldToChunk(), y.worldToChunk()) }
    var Entity.positionComponent by propertyFor(mapper)

    val Entity.physicsX: Float
      get() = positionComponent.x + world.worldBody.worldOffsetX

    val Entity.physicsY: Float
      get() = positionComponent.y + world.worldBody.worldOffsetY

    fun Entity.teleport(worldX: Float, worldY: Float) {
      val position = positionComponent
      position.x = worldX
      position.y = worldY
      updateBox2DPosition = true
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) {
      with(PositionComponent(protoEntity.position.x, protoEntity.position.y))
    }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasPosition()
  }
}
