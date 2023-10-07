package no.elg.infiniteBootleg.world.ecs.components.required

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import ktx.ashley.EngineEntity
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.vector2f
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.WorldCoordNumber
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.util.worldToBlock
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f as ProtoVector2f

data class PositionComponent(var x: Float, var y: Float) : EntitySavableComponent {

  val blockX: WorldCoord get() = worldToBlock(x)
  val blockY: WorldCoord get() = worldToBlock(y)

  fun setPosition(vector2: Vector2) {
    x = vector2.x
    y = vector2.y
  }

  fun toProtoVector2f(): ProtoVector2f =
    vector2f {
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

  companion object : EntityLoadableMapper<PositionComponent>() {
    val Entity.position: Vector2 get() = positionComponent.toVector2()
    val Entity.compactBlockLoc: Long get() = positionComponent.run { compactLoc(blockX, blockY) }
    val Entity.compactChunkLoc: Long get() = positionComponent.run { compactLoc(x.worldToChunk(), y.worldToChunk()) }
    val Entity.positionComponent by propertyFor(mapper)

    fun Entity.teleport(worldX: WorldCoordNumber, worldY: WorldCoordNumber) {
      val position = positionComponent
      position.x = worldX.toFloat()
      position.y = worldY.toFloat()
      updateBox2DPosition = true
      setVelocity(0f, 0f)
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = with(PositionComponent(protoEntity.position.x, protoEntity.position.y))

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasPosition()
  }
}
