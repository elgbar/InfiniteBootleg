package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import ktx.ashley.entity
import ktx.ashley.plusAssign
import ktx.ashley.with
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.world.ClientWorld
import no.elg.infiniteBootleg.world.Constants
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.box2d.Filters
import no.elg.infiniteBootleg.world.ecs.components.DoorComponent
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedMaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEventQueue
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag

const val PLAYER_WIDTH = 1.8f
const val PLAYER_HEIGHT = 3.8f

const val PLAYERS_FOOT_USER_DATA = "A bloody foot!"
const val PLAYERS_RIGHT_ARM_USER_DATA = "Righty"
const val PLAYERS_LEFT_ARM_USER_DATA = "Left hand"
const val ESSENTIALLY_ZERO = 0.001f

private fun createBody2DBodyComponent(
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
  createBody: (Body) -> Unit
) {
  val bodyDef = BodyDef()
  bodyDef.type = bodyType
  bodyDef.position.set(worldX, worldY)
  bodyDef.linearVelocity.set(dx, dy)
  bodyDef.linearDamping = 0.5f
  bodyDef.fixedRotation = true

  world.worldBody.createBody(bodyDef) {
    it.gravityScale = Constants.DEFAULT_GRAVITY_SCALE
    it.userData = entity

    createBody(it)
    entity += Box2DBodyComponent(it, width, height)
    if (Settings.debug) {
      check(basicEntityFamily.matches(entity)) { "Finished entity does not match the basic entity family" }
      wantedFamilies.forEach { (family: Family, errorStr) ->
        check(family.matches(entity)) { "Finished entity does not match $errorStr" }
      }
      Main.logger().log("Finishing setting up box2d entity")
    }
  }
}

fun Engine.createPlayerEntity(world: World, worldX: Float, worldY: Float, dx: Float, dy: Float, name: String = "Player", id: String? = null) = entity {
  with(WorldComponent(world))
  with(id?.let { IdComponent(it) } ?: IdComponent.createRandomId())
  with(PositionComponent(worldX, worldY))

  // BASIC_DYNAMIC_ENTITY_ARRAY
  with(VelocityComponent(dx, dy))
  with<GroundedComponent>()

  // player family
  with(NamedComponent(name))
  with<KillableComponent>()
  with(TextureRegionComponent(KAssets.playerTexture))

  if (world is ClientWorld) {
    with(LocallyControlledComponent(KeyboardControls(world)))
    with<FollowedByCameraTag>()
  }

  // This entity will handle input events
  with<PhysicsEventQueue>()
  with<InputEventQueue>()

  with<LookDirectionComponent>()
  with<SelectedMaterialComponent>()

  createBody2DBodyComponent(
    entity,
    world,
    worldX,
    worldY,
    dx,
    dy,
    PLAYER_WIDTH,
    PLAYER_HEIGHT,
    arrayOf(
      basicDynamicEntityFamily to "basicDynamicEntityFamily",
      drawableEntitiesFamily to "drawableEntitiesFamily",
      playerFamily to "playerFamily",
      controlledEntityFamily to "controlledEntityFamily",
      followEntityFamily to "followEntityFamily",
      entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily",
      controlledEntityWithInputEventFamily to "controlledEntityWithInputEventFamily"
    )
  ) {
    fun createPlayerFixture(body: Body) {
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
      fix.filterData = Filters.EN_GR__ENTITY_FILTER
      fix.userData = body.userData

      shape.dispose()
    }

    fun createSecondaryPlayerFixture(body: Body, userData: String, width: Float, height: Float, rx: Float = 0f, ry: Float = 0f) {
      val shape = PolygonShape()
      shape.setAsBox(width, height, Vector2(rx, ry), 0f)

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

    fun createPlayerTouchAreaFixture(body: Body, userData: String, side: Int) {
      createSecondaryPlayerFixture(body, userData, width = ESSENTIALLY_ZERO, height = PLAYER_HEIGHT / 2f, rx = PLAYER_WIDTH * side / 1.5f)
    }

    createPlayerFixture(it)
    createSecondaryPlayerFixture(it, PLAYERS_FOOT_USER_DATA, width = PLAYER_WIDTH / 3f, height = ESSENTIALLY_ZERO, ry = -(PLAYER_HEIGHT / 2f))
    createPlayerTouchAreaFixture(it, PLAYERS_LEFT_ARM_USER_DATA, -1)
    createPlayerTouchAreaFixture(it, PLAYERS_RIGHT_ARM_USER_DATA, 1)
  }
}

fun Engine.createFallingBlockEntity(world: World, worldX: Float, worldY: Float, dx: Float, dy: Float, material: Material, id: String? = null) = entity {
  with(WorldComponent(world))
  with(id?.let { IdComponent(it) } ?: IdComponent.createRandomId())
  with(PositionComponent(worldX, worldY))

  // BASIC_DYNAMIC_ENTITY_ARRAY
  with(VelocityComponent(dx, dy))

  with(TextureRegionComponent(material.textureRegion ?: error("Failed to get material texture region")))

  // This entity will handle input events
  with<PhysicsEventQueue>()
  with(MaterialComponent(material))

  val bodyDef = BodyDef()
  bodyDef.type = BodyDef.BodyType.DynamicBody
  bodyDef.position.set(worldX, worldY)
  bodyDef.linearVelocity.set(dx, dy)
  bodyDef.linearDamping = 0.5f
  bodyDef.fixedRotation = true

  createBody2DBodyComponent(
    entity, world, worldX, worldY, dx, dy, 1f, 1f,
    arrayOf(
      basicDynamicEntityFamily to "basicDynamicEntityFamily",
      drawableEntitiesFamily to "drawableEntitiesFamily",
      entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily",
      blockEntityFamily to "blockEntityFamily"
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
    fix.filterData = Filters.GR__ENTITY_FILTER
    fix.userData = it.userData

    shape.dispose()
  }
}

fun Engine.createDoorEntity(
  world: World,
  worldX: Float,
  worldY: Float,
  id: String? = null
) = entity {
  with(WorldComponent(world))
  with(id?.let { IdComponent(it) } ?: IdComponent.createRandomId())
  with(PositionComponent(worldX, worldY))

  val door = Material.DOOR
  with(TextureRegionComponent(KAssets.doorClosedTexture))

  // This entity will handle input events
  with(MaterialComponent(door))
  with<DoorComponent>()
  with<PhysicsEventQueue>()

  val width = 2f
  val height = 4f

  createBody2DBodyComponent(
    entity,
    world,
    worldX,
    worldY,
    0f,
    0f,
    width,
    height,
    arrayOf(
      drawableEntitiesFamily to "drawableEntitiesFamily",
      blockEntityFamily to "blockEntityFamily",
      doorEntityFamily to "doorEntityFamily",
      entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily"
    ),
    BodyDef.BodyType.StaticBody
  ) {
    val shape = PolygonShape()
    shape.setAsBox(width / 2, height / 2)

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
