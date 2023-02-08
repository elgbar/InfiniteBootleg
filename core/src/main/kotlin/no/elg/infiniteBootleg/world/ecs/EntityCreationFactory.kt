package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
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
import no.elg.infiniteBootleg.world.Constants
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.box2d.Filters
import no.elg.infiniteBootleg.world.ecs.components.ControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEventQueue
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag

const val PLAYER_WIDTH = 2f
const val PLAYER_HEIGHT = 4f

const val PLAYERS_FOOT_USER_DATA = "A bloody foot!"
const val PLAYERS_RIGHT_ARM_USER_DATA = "Righty"
const val PLAYERS_LEFT_ARM_USER_DATA = "Left hand"
const val ESSENTIALLY_ZERO = 0.001f

private fun createBody2DBodyComponent(entity: Entity, world: World, worldX: Float, worldY: Float, dx: Float, dy: Float) {
  val bodyDef = BodyDef()
  bodyDef.type = BodyDef.BodyType.DynamicBody
  bodyDef.position.set(worldX, worldY)
  bodyDef.linearVelocity.set(dx, dy)
  bodyDef.linearDamping = 1f
  bodyDef.fixedRotation = true

  world.worldBody.createBody(bodyDef) {
    it.gravityScale = Constants.DEFAULT_GRAVITY_SCALE
    it.userData = entity

    createPlayerFixture(it)
    createPlayerFootFixture(it)
    createPlayerTouchAreaFixture(it, PLAYERS_LEFT_ARM_USER_DATA, -1)
    createPlayerTouchAreaFixture(it, PLAYERS_RIGHT_ARM_USER_DATA, 1)

    entity += Box2DBodyComponent(it, PLAYER_WIDTH, PLAYER_HEIGHT)
    Main.logger().log("Finishing setting up box2d entity")
  }
}

private fun createPlayerFixture(body: Body) {
  val shape = PolygonShape()

  shape.setAsBox(PLAYER_WIDTH / 2f, PLAYER_HEIGHT / 2f)

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

private fun createPlayerFootFixture(body: Body) {
  createSecondaryPlayerFixture(body, PLAYERS_FOOT_USER_DATA, height = ESSENTIALLY_ZERO, ry = -PLAYER_HEIGHT / 2f)
}

private fun createPlayerTouchAreaFixture(body: Body, userData: String, side: Int) {
  createSecondaryPlayerFixture(body, userData, width = ESSENTIALLY_ZERO, rx = PLAYER_WIDTH * side / 1.75f)
}

private fun createSecondaryPlayerFixture(body: Body, userData: String, width: Float = PLAYER_WIDTH / 2.1f, height: Float = PLAYER_HEIGHT / 2.1f, rx: Float = 0f, ry: Float = 0f) {
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

  with(ControlledComponent.LocallyControlledComponent)
  with<FollowedByCameraTag>()

  // This entity will handle input events
  with<PhysicsEventQueue>()
  with<InputEventQueue>()

  createBody2DBodyComponent(entity, world, worldX, worldY, dx, dy)
}
