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
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.util.worldToBlock
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f as ProtoVector2f

data class PositionComponent(var x: Float, var y: Float) : EntitySavableComponent {

  private val pos by lazy { Vector2(x, y) }

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

  /**
   * Returns the same vector each time
   */
  fun toVector2(): Vector2 = pos.set(x, y)

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

    /**
     * Teleport the entity to the given world coordinates
     *
     * @param worldX The world x coordinate to teleport to
     * @param worldY The world y coordinate to teleport to
     * @param killVelocity If the velocity of the entity should be set to zero
     */
    fun Entity.teleport(worldX: WorldCoordNumber, worldY: WorldCoordNumber, killVelocity: Boolean = false) {
      val position = positionComponent
      position.x = worldX.toFloat()
      position.y = worldY.toFloat()
      updateBox2DPosition = true
      if (killVelocity) {
        setVelocity(0f, 0f)
      }
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = safeWith { PositionComponent(protoEntity.position.x, protoEntity.position.y) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasPosition()
  }
}
