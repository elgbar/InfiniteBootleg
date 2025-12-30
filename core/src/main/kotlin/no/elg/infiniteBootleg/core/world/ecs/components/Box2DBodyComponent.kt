package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.box2d.structs.b2BodyId
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.math.component1
import ktx.math.component2
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.Constants
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.box2d.extensions.gravityScale
import no.elg.infiniteBootleg.core.world.box2d.extensions.isValid
import no.elg.infiniteBootleg.core.world.box2d.extensions.position
import no.elg.infiniteBootleg.core.world.box2d.extensions.userData
import no.elg.infiniteBootleg.core.world.box2d.extensions.velocity
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.LoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityOrZero
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FlyingTag.Companion.ensureFlyingStatus
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

private val logger = KotlinLogging.logger {}

/**
 *
 * @param box2dWidth  The height of this entity in box2d view
 * @param box2dHeight The width of this entity in box2d view
 */
class Box2DBodyComponent(body: b2BodyId, val type: ProtoWorld.Entity.Box2D.BodyType, val box2dWidth: Float, val box2dHeight: Float, val fixedRotation: Boolean) :
  EntitySavableComponent,
  CheckableDisposable {

  private var internalBody: b2BodyId? = body
    get() {
      val field = field ?: return null
      return if (field.isValid) {
        field
      } else {
        logger.warn { "Body was disposed without the Box2DBodyComponent noticing it, ${hudDebug()}" }
        disposed = true
        null
      }
    }
  private var disposed = false

  val halfBox2dWidth: Float get() = box2dWidth / 2f
  val halfBox2dHeight: Float get() = box2dHeight / 2f

  val worldWidth: Float get() = box2dWidth * Block.BLOCK_TEXTURE_SIZE
  val worldHeight: Float get() = box2dHeight * Block.BLOCK_TEXTURE_SIZE

  override val isDisposed get() = disposed

  val body: b2BodyId
    get() {
      val bodyId = if (disposed) {
        error("Tried to access a disposed body!")
      } else {
        internalBody
      }
      return bodyId ?: error("Tried to access an invalid or null body!")
    }

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

  override fun hudDebug(): String {
    val box2dBody = body
    val size = stringifyCompactLoc(box2dWidth, box2dHeight)
    val pos = stringifyCompactLoc(box2dBody.position)
    val vel = stringifyCompactLoc(box2dBody.velocity)
    return "type: $type, gravity ${box2dBody.gravityScale}, size $size, pos $pos, vel $vel, disposed? $disposed, userdata ${box2dBody.userData}"
  }

  companion object : LoadableMapper<Box2DBodyComponent, ProtoWorld.Entity, (Entity) -> Unit>() {
    val Entity.box2dBody get() = box2d.body
    val Entity.box2d by propertyFor(mapper)
    val Entity.box2dOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity, state: (Entity) -> Unit): Box2DBodyComponent? {
      val world = entity.world
      val (worldX, worldY) = entity.positionComponent
      val (velX, velY) = entity.velocityOrZero()
      when (protoEntity.box2D.bodyType) {
        PLAYER -> createPlayerBodyComponent(world, worldX, worldY, velX, velY, NON_CONTROLLED_PLAYER_FAMILIES) { entity ->
          entity.ensureFlyingStatus()
          state(entity)
        }

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
