package no.elg.infiniteBootleg.world.ecs.components.required

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import ktx.ashley.Mapper
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.vector2f
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.worldToBlock
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f as ProtoVector2f

data class PositionComponent(var x: Float, var y: Float) : Component {

  val blockX: Int get() = worldToBlock(x)
  val blockY: Int get() = worldToBlock(y)

  fun toProtoVector2f(): ProtoVector2f = vector2f {
    x = this@PositionComponent.x
    y = this@PositionComponent.y
  }

  fun toVector2(): Vector2 = Vector2(x, y)

  companion object : Mapper<PositionComponent>() {
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
  }
}
