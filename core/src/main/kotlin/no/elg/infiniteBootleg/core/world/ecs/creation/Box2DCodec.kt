package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.enums.b2BodyType
import com.badlogic.gdx.box2d.structs.b2BodyDef
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2Capsule
import com.badlogic.gdx.box2d.structs.b2Circle
import com.badlogic.gdx.box2d.structs.b2Polygon
import com.badlogic.gdx.box2d.structs.b2ShapeDef
import com.badlogic.gdx.math.Vector2
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.ashley.EngineEntity
import ktx.ashley.plusAssign
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.isBeingRemoved
import no.elg.infiniteBootleg.core.util.toRadians
import no.elg.infiniteBootleg.core.world.Constants
import no.elg.infiniteBootleg.core.world.box2d.Filters
import no.elg.infiniteBootleg.core.world.box2d.NO_ROTATION
import no.elg.infiniteBootleg.core.world.box2d.extensions.createCapsuleShape
import no.elg.infiniteBootleg.core.world.box2d.extensions.createCircleShape
import no.elg.infiniteBootleg.core.world.box2d.extensions.createPolygonShape
import no.elg.infiniteBootleg.core.world.box2d.extensions.makeB2Vec2
import no.elg.infiniteBootleg.core.world.box2d.extensions.set
import no.elg.infiniteBootleg.core.world.box2d.extensions.userData
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.blockEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.doorEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.core.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.core.world.ecs.standaloneGridOccupyingBlocksFamily
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}
// ////////////
// CREATION //
// ////////////

fun EngineEntity.createPlayerBodyComponent(
  world: World,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  wantedFamilies: Array<Pair<Family, String>>,
  whenReady: (Entity) -> Unit = {}
) {
  createBody2DBodyComponent(
    ProtoWorld.Entity.Box2D.BodyType.PLAYER,
    entity,
    world,
    worldX,
    worldY,
    dx,
    dy,
    PLAYER_WIDTH,
    PLAYER_HEIGHT,
    wantedFamilies,
    bodyDefModifier = {
//      linearDamping(0.5f)
      isBullet(true)
    },
    afterBodyComponentAdded = whenReady
  ) {
    val body = this
    createPlayerCapsule(body, entity) {
//      set(playerVertices)
      b2Capsule().apply {
        val halfWidth = PLAYER_WIDTH / 2f
        center1 = center1.set(0f, halfWidth - (PLAYER_HEIGHT / 2f))
        center2 = center2.set(0f, (PLAYER_HEIGHT / 2f) - halfWidth)
        radius(halfWidth)
      }
    }
//    createPlayerPolygon(body, PLAYERS_FOOT_USER_DATA) {
//      Box2d.b2MakeOffsetBox(
//        /* halfWidth = */
//        PLAYER_WIDTH / 4f,
//        /* halfHeight = */
//        ESSENTIALLY_ZERO,
//        /* center = */
//        makeB2Vec2(0f, -PLAYER_HEIGHT / 2f - A_LITTLE_MORE_THAN_ESSENTIALLY_ZERO),
//        /* rotation = */
//        NO_ROTATION
//      )
//    }
    val radius = 0.5f
    createSecondaryPlayerFixture(body, HOLE_DETECTOR_USER_DATA, halfWidth = radius, halfHeight = radius, centerY = -(PLAYER_HEIGHT) / 2f - radius)
    createPlayerTouchAreaFixture(body, PLAYERS_LEFT_ARM_USER_DATA, -1)
    createPlayerTouchAreaFixture(body, PLAYERS_RIGHT_ARM_USER_DATA, 1)
  }
}

fun EngineEntity.createDoorBodyComponent(world: World, worldX: WorldCoord, worldY: WorldCoord, whenReady: (Entity) -> Unit = {}) {
  createBody2DBodyComponent(
    ProtoWorld.Entity.Box2D.BodyType.DOOR,
    entity,
    world,
    worldX.toFloat() + DOOR_WIDTH / 2f,
    worldY.toFloat() + DOOR_HEIGHT / 2f,
    0f,
    0f,
    DOOR_WIDTH.toFloat(),
    DOOR_HEIGHT.toFloat(),
    arrayOf(
      drawableEntitiesFamily to "drawableEntitiesFamily",
      blockEntityFamily to "blockEntityFamily",
      doorEntityFamily to "doorEntityFamily",
      entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily",
      standaloneGridOccupyingBlocksFamily to "standaloneGridOccupyingBlocksFamily"
    ),
    bodyDefModifier = {
      type(b2BodyType.b2_staticBody)
    },
    afterBodyComponentAdded = whenReady
  ) {
    val polygon = Box2d.b2MakeBox(DOOR_WIDTH / 2f, DOOR_HEIGHT / 2f)
    val shapeDef = Box2d.b2DefaultShapeDef().also { def ->
      def.isSensor(true)
      def.enableSensorEvents(true)
      def.filter = Filters.EN__GROUND_FILTER
    }

    createPolygonShape(shapeDef, polygon, entity)
  }
}

fun EngineEntity.createFallingBlockBodyComponent(
  world: World,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  onReady: (Entity) -> Unit = {}
) {
  createBody2DBodyComponent(
    serializationType = ProtoWorld.Entity.Box2D.BodyType.FALLING_BLOCK,
    entity = entity,
    world = world,
    worldX = worldX,
    worldY = worldY,
    dx = dx,
    dy = dy,
    width = 1f,
    height = 1f,
    wantedFamilies = arrayOf(
      basicDynamicEntityFamily to "basicDynamicEntityFamily",
      drawableEntitiesFamily to "drawableEntitiesFamily",
      entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily",
      standaloneGridOccupyingBlocksFamily to "standaloneGridOccupyingBlocksFamily"
    ),
    afterBodyComponentAdded = onReady
  ) {
    val polygon = Box2d.b2MakeSquare(0.45f)
    val shapeDef = Box2d.b2DefaultShapeDef().also { def ->
      def.isSensor(true)
      def.enableSensorEvents(true)
      def.filter = Filters.GR_FB__FALLING_BLOCK_FILTER
      def.material().apply {
        restitution(0f)
        friction(Constants.DEFAULT_FIXTURE_FRICTION)
      }
    }
    createPolygonShape(shapeDef, polygon, entity)
  }
}

fun EngineEntity.createSpellBodyComponent(
  world: World,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  onReady: (Entity) -> Unit = {}
) {
  val size = 0.25f
  createBody2DBodyComponent(
    ProtoWorld.Entity.Box2D.BodyType.SPELL,
    entity,
    world,
    worldX,
    worldY,
    dx,
    dy,
    size,
    size,
    arrayOf(
      basicDynamicEntityFamily to "basicDynamicEntityFamily",
      drawableEntitiesFamily to "drawableEntitiesFamily",
      entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily"
    ),
    bodyDefModifier = {
      this.isBullet(true)
      fixedRotation(false)
      angularVelocity((90f * sqrt(dx * dx + dy * dy)).toRadians())
      angularDamping(0f)
      rotation(Box2d.b2MakeRot(22.5f.toRadians()))
      gravityScale(0f)
    },
    afterBodyComponentAdded = onReady
  ) {
    val circle = b2Circle().apply {
      radius(size)
    }
    val shapeDef = Box2d.b2DefaultShapeDef().also { def ->
      def.isSensor(true)
      def.enableSensorEvents(true)
      def.filter = Filters.GR_FB__FALLING_BLOCK_FILTER
    }
    createCircleShape(shapeDef, circle, entity)
  }
}

// /////////
// UTILS //
// /////////

internal fun createBody2DBodyComponent(
  serializationType: ProtoWorld.Entity.Box2D.BodyType,
  entity: Entity,
  world: World,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  width: Float,
  height: Float,
  wantedFamilies: Array<Pair<Family, String>> = emptyArray(),
  bodyDefModifier: b2BodyDef.() -> Unit = {},
  afterBodyComponentAdded: (Entity) -> Unit = {},
  beforeBodyComponentAdded: b2BodyId.() -> Unit
) {
  val bodyDef: b2BodyDef = Box2d.b2DefaultBodyDef()
  bodyDef.type(b2BodyType.b2_dynamicBody)
  bodyDef.position = makeB2Vec2(worldX + 0.5f, worldY + 0.5f)
  bodyDef.linearVelocity = makeB2Vec2(dx, dy)
  val fixedRotation = true
  bodyDef.fixedRotation(fixedRotation)
  bodyDefModifier(bodyDef)

  world.worldBody.createBody(bodyDef) {
    if (entity.isBeingRemoved) {
      // If the entity was removed before the body was created, destroy the body
      entity.world.worldBody.destroyBody(it)
      return@createBody
    }
    it.userData = entity

    beforeBodyComponentAdded(it)
    entity += Box2DBodyComponent(it, serializationType, width, height, fixedRotation)
    afterBodyComponentAdded(entity)
    if (Settings.debug) {
      check(basicStandaloneEntityFamily.matches(entity)) { "Finished entity does not match the basic entity family" }
      checkFamilies(entity, wantedFamilies)
    }
  }
}

private fun createPlayerCapsule(body: b2BodyId, userData: Any, friction: Float = Constants.DEFAULT_FIXTURE_FRICTION, defineShape: () -> b2Capsule) {
  val shape = defineShape()
  playerShapeDef.material().friction(friction)
  body.createCapsuleShape(playerShapeDef, shape, userData)
}

private fun createPlayerPolygon(body: b2BodyId, userData: Any, friction: Float = Constants.DEFAULT_FIXTURE_FRICTION, defineShape: () -> b2Polygon) {
  val shape = defineShape()
  playerShapeDef.material().friction(friction)

  body.createPolygonShape(playerShapeDef, shape, userData)
}

private fun createSecondaryPlayerFixture(
  body: b2BodyId,
  userData: String,
  halfWidth: Float,
  halfHeight: Float,
  centerX: Float = 0f,
  centerY: Float = 0f
) {
  val polygon = Box2d.b2MakeOffsetBox(
    halfWidth.coerceAtLeast(ESSENTIALLY_ZERO),
    halfHeight.coerceAtLeast(ESSENTIALLY_ZERO),
    makeB2Vec2(centerX, centerY),
    NO_ROTATION
  )

  val shapeDef = Box2d.b2DefaultShapeDef().apply {
    isSensor(true)
    enableSensorEvents(true)
    filter = Filters.GR__ENTITY_FILTER
  }

  body.createPolygonShape(shapeDef, polygon, userData)
}

private fun createPlayerTouchAreaFixture(body: b2BodyId, userData: String, side: Int) {
  createSecondaryPlayerFixture(body, userData, halfWidth = ESSENTIALLY_ZERO, halfHeight = PLAYER_HEIGHT / 2.3f, centerX = PLAYER_WIDTH * side / 1.5f)
}

private val playerShapeDef: b2ShapeDef = Box2d.b2DefaultShapeDef().also { def ->
  def.filter = Filters.GR_EN_ENTITY_FILTER
//  def.fixedRotation(true)
//  density = Constants.DEFAULT_FIXTURE_DENSITY
  def.material().restitution(0f)
}

private val playerVertices = Array(8) { Vector2() }.also { vertices ->
  val halfWidth = PLAYER_WIDTH / 2f
  val halfHeight = PLAYER_HEIGHT / 2f
  val nearZW = halfWidth - 0.1f
  val nearZH = halfHeight - 0.1f

  vertices[0].set(-nearZW, halfHeight)
  vertices[1].set(nearZW, halfHeight)
  vertices[2].set(-halfWidth, -(halfHeight / 2f))
  vertices[3].set(-halfWidth, nearZH)
  vertices[4].set(-(halfWidth / 2f), -halfHeight + ESSENTIALLY_ZERO)
  vertices[5].set((halfWidth / 2f), -halfHeight + ESSENTIALLY_ZERO)
  vertices[6].set(halfWidth, -(halfHeight / 2f))
  vertices[7].set(halfWidth, nearZH)
}
