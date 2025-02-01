package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.Body
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.math.component1
import ktx.math.component2
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.Constants
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.LoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityOrZero
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.creation.NON_CONTROLLED_PLAYER_FAMILIES
import no.elg.infiniteBootleg.core.world.ecs.creation.createDoorBodyComponent
import no.elg.infiniteBootleg.core.world.ecs.creation.createFallingBlockBodyComponent
import no.elg.infiniteBootleg.core.world.ecs.creation.createPlayerBodyComponent
import no.elg.infiniteBootleg.core.world.ecs.creation.createSpellBodyComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.box2D
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Box2D.BodyType.DOOR
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Box2D.BodyType.FALLING_BLOCK
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Box2D.BodyType.PLAYER
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Box2D.BodyType.SPELL

/**
 *
 * @param box2dWidth  The height of this entity in box2d view
 * @param box2dHeight The width of this entity in box2d view
 */
class Box2DBodyComponent(
  body: Body,
  val type: ProtoWorld.Entity.Box2D.BodyType,
  private val box2dWidth: Float,
  private val box2dHeight: Float
) : EntitySavableComponent, CheckableDisposable {

  private var internalBody: Body? = body
  private var disposed = false

  val halfBox2dWidth: Float get() = box2dWidth / 2f
  val halfBox2dHeight: Float get() = box2dHeight / 2f

  val worldWidth: Float get() = box2dWidth * Block.Companion.BLOCK_TEXTURE_SIZE
  val worldHeight: Float get() = box2dHeight * Block.Companion.BLOCK_TEXTURE_SIZE

  override val isDisposed get() = disposed

  val body: Body = (if (disposed) null else internalBody) ?: error("Tried to access a disposed body!")

  fun disableGravity() {
    body.gravityScale = 0f
  }

  fun enableGravity() {
    body.gravityScale = Constants.DEFAULT_GRAVITY_SCALE
  }

  override fun dispose() {
    if (!isDisposed) {
      disposed = true
      val currentBody = internalBody ?: return
      this.internalBody = null
      val entity = currentBody.userData as Entity
      entity.world.worldBody.destroyBody(currentBody)
    }
  }

  override fun hudDebug(): String =
    "type: $type, gravity ${body.gravityScale}, size ${stringifyCompactLoc(box2dWidth, box2dHeight)}, disposed? $disposed, userdata ${internalBody?.userData}"

  companion object : LoadableMapper<Box2DBodyComponent, ProtoWorld.Entity, (Entity) -> Unit>() {
    val Entity.box2dBody get() = box2d.body
    val Entity.box2d by propertyFor(mapper)
    val Entity.box2dOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity, state: (Entity) -> Unit): Box2DBodyComponent? {
      val world = entity.world
      val (worldX, worldY) = entity.position
      val (velX, velY) = entity.velocityOrZero
      when (protoEntity.box2D.bodyType) {
        PLAYER -> createPlayerBodyComponent(world, worldX, worldY, velX, velY, NON_CONTROLLED_PLAYER_FAMILIES, state)
        FALLING_BLOCK -> createFallingBlockBodyComponent(world, worldX, worldY, velX, velY, state)
        DOOR -> createDoorBodyComponent(world, worldX.toInt(), worldY.toInt(), state)
        SPELL -> createSpellBodyComponent(world, worldX, worldY, velX, velY, state)
        else -> error("Unknown body type ${protoEntity.box2D.bodyType}")
      }
      return null
    }

    override fun ProtoWorld.Entity.checkShouldLoad(state: () -> (Entity) -> Unit): Boolean = hasBox2D()
  }

  override fun EntityKt.Dsl.save() {
    box2D = box2D {
      bodyType = this@Box2DBodyComponent.type
    }
  }
}
