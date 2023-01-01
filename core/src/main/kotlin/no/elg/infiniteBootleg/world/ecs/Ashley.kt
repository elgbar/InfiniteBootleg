package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import ktx.ashley.Mapper
import ktx.ashley.allOf
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.box2d.WorldBody.Companion.X_WORLD_GRAVITY
import no.elg.infiniteBootleg.world.box2d.WorldBody.Companion.Y_WORLD_GRAVITY

/**
 * The entity is affected by gravity
 */
class GravityComp(var dx: Float = X_WORLD_GRAVITY, var dy: Float = Y_WORLD_GRAVITY) : Component {
  companion object : Mapper<GravityComp>()
}

class TextureComp(var texture: TextureRegion) : Component {
  companion object : Mapper<TextureComp>()
}

class MaterialComp(var material: Material) : Component {
  companion object : Mapper<MaterialComp>()
}

class PositionComp(val pos: Vector2) : Component {

  val x get() = pos.x
  val y get() = pos.y

  fun getPhysicsPosition(): Vector2 {
    return posCache
      .cpy()
      .add(world.getWorldBody().worldOffsetX, world.getWorldBody().worldOffsetY)
  }

  companion object : Mapper<PositionComp>()
}

data class VelocityComp(
  val velocity: Vector2,
  var maxDx: Float = KeyboardControls.MAX_X_VEL,
  var maxDy: Float = KeyboardControls.MAX_X_VEL
) : Component {

  val dx get() = velocity.x
  val dy get() = velocity.y

  init {
    require(maxDx > 0) { "Max dx velocity must be strictly positive" }
    require(maxDy > 0) { "Max dy velocity must be strictly positive" }
  }

  companion object : Mapper<VelocityComp>()
}

class WorldComp(val world: World) : Component {
  companion object : Mapper<WorldComp>()
}

/**
 * One unit is [Block.BLOCK_SIZE]
 *
 * @param width  The height of this entity in world view
 * @param height The width of this entity in world view
 */
data class SizeComp(val width: Float, val height: Float) : Component {

  val halfBox2dWidth: Float get() = width / (Block.BLOCK_SIZE * 2f)

  val halfBox2dHeight: Float get() = height / (Block.BLOCK_SIZE * 2f)

  companion object : Mapper<SizeComp>()
}

data class Box2DComp(val body: Body) : Component {
  companion object : Mapper<Box2DComp>()
}

data class BodyDefComp(val bodyDef: BodyDef = DEFAULT) : Component {
  companion object : Mapper<Box2DComp>() {
    val DEFAULT by lazy {
      BodyDef().also {
        it.type = BodyDef.BodyType.DynamicBody
        it.linearDamping = 1f
        it.fixedRotation = true
      }
    }
  }
}

/**
 * Center the entity when spawned
 */
class CenterOnSpawn : Component {
  companion object : Mapper<CenterOnSpawn>()
}

data class AliveComp(var health: Int, var maxHealth: Int) : Component {
  companion object : Mapper<AliveComp>()
}

data class NamedComp(val name: String) : Component {
  companion object : Mapper<NamedComp>()
}

class ControlledComp : Component {
  companion object : Mapper<ControlledComp>()
}

class FollowedByCameraComp : Component {
  companion object : Mapper<FollowedByCameraComp>()
}

data class OnGroundComp(var contactPoints: Int) : Component {

  operator fun plusAssign(toAdd: Int) {
    contactPoints += toAdd
  }

  operator fun minusAssign(toRemove: Int) {
    contactPoints -= toRemove
  }

  val onGround: Boolean get() = contactPoints == 0

  companion object : Mapper<OnGroundComp>()
}

/**
 * The list of components all entities are expected to have
 */
val BASIC_ENTITY_ARRAY = arrayOf(
  WorldComp::class,
  SizeComp::class,
  Box2DComp::class,
  VelocityComp::class,
  PositionComp::class,
  TextureComp::class,
  OnGroundComp::class
)

val blockEntityFamily: Family = allOf(*BASIC_ENTITY_ARRAY).allOf(MaterialComp::class).get()

/**
 * The basic components ALL entities should have
 */
val basicEntityFamily: Family = allOf(*BASIC_ENTITY_ARRAY).get()

var Entity.world by propertyFor(WorldComp.mapper)
var Entity.position by propertyFor(PositionComp.mapper)
var Entity.velocity by propertyFor(VelocityComp.mapper)
var Entity.box2d by propertyFor(Box2DComp.mapper)
var Entity.box2dOrNull by optionalPropertyFor(Box2DComp.mapper)
var Entity.size by propertyFor(SizeComp.mapper)
var Entity.texture by propertyFor(TextureComp.mapper)
var Entity.onGround by propertyFor(OnGroundComp.mapper)

fun createBasicEntity(world: World, bodyDef: BodyDef)

// class InventoryComp()
