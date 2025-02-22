package no.elg.infiniteBootleg.core.world.ecs.components.required

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import ktx.ashley.EngineEntity
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.WorldCoordFloat
import no.elg.infiniteBootleg.core.util.WorldCoordNumber
import no.elg.infiniteBootleg.core.util.compactLoc
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.util.worldToBlock
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.UpdateBox2DPositionTag.Companion.updateBox2DPosition
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.vector2f
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f as ProtoVector2f

data class PositionComponent(var x: WorldCoordFloat, var y: WorldCoordFloat) : EntitySavableComponent {

  private val pos by lazy { Vector2(x, y) }

  val blockX: WorldCoord get() = x.worldToBlock()
  val blockY: WorldCoord get() = y.worldToBlock()
  val chunkX: WorldCoord get() = x.worldToChunk()
  val chunkY: WorldCoord get() = y.worldToChunk()

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

  override fun hudDebug(): String = stringifyCompactLoc(x, y)

  companion object : EntityLoadableMapper<PositionComponent>() {
    val Entity.position: Vector2 get() = positionComponent.toVector2()
    val Entity.compactBlockLoc: Long get() = positionComponent.run { compactLoc(blockX, blockY) }
    val Entity.compactChunkLoc: Long get() = positionComponent.run { compactLoc(chunkX, chunkY) }
    val Entity.positionComponent by propertyFor(mapper)

    /**
     * Teleport the entity to the given world coordinates
     *
     * @param pos The world coordinates to teleport to
     * @param killVelocity If the velocity of the entity should be set to zero
     */
    inline fun Entity.teleport(pos: ProtoWorld.Vector2f, killVelocity: Boolean = false) = teleport(pos.x, pos.y, killVelocity)

    /**
     * Teleport the entity to the given world coordinates
     *
     * @param pos The world coordinates to teleport to
     * @param killVelocity If the velocity of the entity should be set to zero
     */
    inline fun Entity.teleport(pos: Vector2, killVelocity: Boolean = false) = teleport(pos.x, pos.y, killVelocity)

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

    /**
     * Get the chunk of this entity or null if the chunk is invalid
     *
     * @param load If the chunk should be loaded, default is **false** This is different from the default behavior of [no.elg.infiniteBootleg.core.world.world.World.getChunk]
     */
    fun Entity.getChunkOrNull(load: Boolean = false): Chunk? = world.getChunk(compactChunkLoc, load)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = safeWith { PositionComponent(protoEntity.position.x, protoEntity.position.y) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasPosition()
  }
}
