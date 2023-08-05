package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import ktx.ashley.EngineEntity
import ktx.ashley.plusAssign
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.Constants
import no.elg.infiniteBootleg.world.box2d.Filters
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.world.ecs.blockEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.doorEntityFamily
import no.elg.infiniteBootleg.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.world.ecs.standaloneGridOccupyingBlocksFamily
import no.elg.infiniteBootleg.world.world.World

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
    afterBodyCreated = whenReady
  ) {
    createPlayerFixture(it)
    createSecondaryPlayerFixture(it, PLAYERS_FOOT_USER_DATA, width = PLAYER_WIDTH / 3f, height = ESSENTIALLY_ZERO, ry = -(PLAYER_HEIGHT / 2f))
    createPlayerTouchAreaFixture(it, PLAYERS_LEFT_ARM_USER_DATA, -1)
    createPlayerTouchAreaFixture(it, PLAYERS_RIGHT_ARM_USER_DATA, 1)
  }
}

fun EngineEntity.createDoorBodyComponent(world: World, worldX: Int, worldY: Int, whenReady: (Entity) -> Unit = {}) {
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
    BodyDef.BodyType.StaticBody,
    whenReady
  ) {
    val shape = PolygonShape()
    shape.setAsBox(DOOR_WIDTH / 2f, DOOR_HEIGHT / 2f)

    val def = FixtureDef()
    def.shape = shape
    def.density = Constants.DEFAULT_FIXTURE_DENSITY
    def.friction = Constants.DEFAULT_FIXTURE_FRICTION
    def.restitution = 0f

    val fix: Fixture = it.createFixture(def)
    fix.filterData = Filters.EN__GROUND_FILTER
    fix.userData = it.userData
    fix.isSensor = true

    shape.dispose()
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
    ProtoWorld.Entity.Box2D.BodyType.FALLING_BLOCK,
    entity,
    world,
    worldX,
    worldY,
    dx,
    dy,
    1f,
    1f,
    arrayOf(
      basicDynamicEntityFamily to "basicDynamicEntityFamily",
      drawableEntitiesFamily to "drawableEntitiesFamily",
      entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily",
      standaloneGridOccupyingBlocksFamily to "standaloneGridOccupyingBlocksFamily"
    )
  ) {
    val shape = PolygonShape()
    shape.setAsBox(0.45f, 0.45f)

    val def = FixtureDef()
    def.shape = shape
    def.density = Constants.DEFAULT_FIXTURE_DENSITY
    def.friction = Constants.DEFAULT_FIXTURE_FRICTION
    def.restitution = 0f

    val fix: Fixture = it.createFixture(def)
    fix.filterData = Filters.GR_FB__FALLING_BLOCK_FILTER
    fix.userData = it.userData
    shape.dispose()
    onReady(this.entity)
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
  bodyType: BodyDef.BodyType = BodyDef.BodyType.DynamicBody,
  afterBodyCreated: (Entity) -> Unit = {},
  createBody: (Body) -> Unit
) {
  val bodyDef = BodyDef()
  bodyDef.type = bodyType
  bodyDef.position.set(worldX, worldY)
  bodyDef.linearVelocity.set(dx, dy)
  bodyDef.linearDamping = 0.5f
  bodyDef.fixedRotation = true

  world.worldBody.createBody(bodyDef) {
    if (entity.isRemoving || entity.isScheduledForRemoval) {
      // If the entity was removed before the body was created, destroy the body
      entity.world.worldBody.destroyBody(it)
      return@createBody
    }

    it.gravityScale = Constants.DEFAULT_GRAVITY_SCALE
    it.userData = entity

    createBody(it)
    entity += Box2DBodyComponent(it, serializationType, width, height)
    if (Settings.debug) {
      check(basicStandaloneEntityFamily.matches(entity)) { "Finished entity does not match the basic entity family" }
      checkFamilies(entity, wantedFamilies)
      Main.logger().debug("BOX2D", "Finishing setting up box2d entity")
    }
    afterBodyCreated(entity)
  }
}

private fun createPlayerFixture(body: Body) {
  val shape = PolygonShape()

  val halfWidth = PLAYER_WIDTH / 2f
  val halfHeight = PLAYER_HEIGHT / 2f
  val nearZW = halfWidth - 0.1f
  val nearZH = halfHeight - 0.1f

  val vertices = Array(8) { Vector2() }
  vertices[0].set(-nearZW, halfHeight)
  vertices[1].set(nearZW, halfHeight)
  vertices[2].set(-halfWidth, -(halfHeight / 2f))
  vertices[3].set(-halfWidth, nearZH)
  vertices[4].set(-(halfWidth / 2f), -halfHeight)
  vertices[5].set((halfWidth / 2f), -halfHeight)
  vertices[6].set(halfWidth, -(halfHeight / 2f))
  vertices[7].set(halfWidth, nearZH)
  shape.set(vertices)

  val def = FixtureDef()
  def.shape = shape
  def.density = Constants.DEFAULT_FIXTURE_DENSITY
  def.friction = Constants.DEFAULT_FIXTURE_FRICTION
  def.restitution = Constants.DEFAULT_FIXTURE_RESTITUTION // a bit bouncy!

  val fix: Fixture = body.createFixture(def)
  fix.filterData = Filters.GR_EN_ENTITY_FILTER
  fix.userData = body.userData

  shape.dispose()
}

private fun createSecondaryPlayerFixture(body: Body, userData: String, width: Float, height: Float, rx: Float = 0f, ry: Float = 0f) {
  val shape = PolygonShape()
  shape.setAsBox(width.coerceAtLeast(ESSENTIALLY_ZERO), height.coerceAtLeast(ESSENTIALLY_ZERO), Vector2(rx, ry), 0f)

  val def = FixtureDef().apply {
    this.shape = shape
    isSensor = true
    filter.set(Filters.GR__ENTITY_FILTER)
  }
  body.createFixture(def).apply {
    this.userData = userData
  }
  shape.dispose()
}

private fun createPlayerTouchAreaFixture(body: Body, userData: String, side: Int) {
  createSecondaryPlayerFixture(body, userData, width = ESSENTIALLY_ZERO, height = PLAYER_HEIGHT / 2f, rx = PLAYER_WIDTH * side / 1.5f)
}
