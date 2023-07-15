package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import ktx.ashley.EngineEntity
import ktx.ashley.entity
import ktx.ashley.with
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.livingOrNull
import no.elg.infiniteBootleg.protobuf.playerOrNull
import no.elg.infiniteBootleg.server.SharedInformation
import no.elg.infiniteBootleg.world.Constants
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.box2d.Filters
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.components.SharedInformationComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEventQueue
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag
import no.elg.infiniteBootleg.world.ecs.controlledEntityFamily
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithInputEventFamily
import no.elg.infiniteBootleg.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.world.ecs.followEntityFamily
import no.elg.infiniteBootleg.world.ecs.localPlayerFamily
import no.elg.infiniteBootleg.world.ecs.playerFamily
import no.elg.infiniteBootleg.world.ecs.with
import no.elg.infiniteBootleg.world.world.ClientWorld
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import no.elg.infiniteBootleg.world.world.ServerWorld
import no.elg.infiniteBootleg.world.world.SinglePlayerWorld
import no.elg.infiniteBootleg.world.world.World
import java.util.UUID
import java.util.concurrent.CompletableFuture

const val PLAYER_WIDTH = 2f - 0.2f
const val PLAYER_HEIGHT = 4f - 0.2f
const val PLAYERS_FOOT_USER_DATA = "A bloody foot!"
const val PLAYERS_RIGHT_ARM_USER_DATA = "Righty"
const val PLAYERS_LEFT_ARM_USER_DATA = "Left hand"

val COMMON_PLAYER_FAMILIES: Array<Pair<Family, String>> = arrayOf(
  playerFamily to "playerFamily",
  basicDynamicEntityFamily to "basicDynamicEntityFamily"
)
val CLIENT_PLAYER_FAMILIES: Array<Pair<Family, String>> = arrayOf(
  *COMMON_PLAYER_FAMILIES,
  drawableEntitiesFamily to "drawableEntitiesFamily",
  entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily"
)
val CONTROLLED_CLIENT_PLAYER_FAMILIES: Array<Pair<Family, String>> = arrayOf(
  *CLIENT_PLAYER_FAMILIES,
  drawableEntitiesFamily to "drawableEntitiesFamily",
  localPlayerFamily to "localPlayerFamily",
  controlledEntityFamily to "controlledEntityFamily",
  followEntityFamily to "followEntityFamily",
  entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily",
  controlledEntityWithInputEventFamily to "controlledEntityWithInputEventFamily"
)

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

private fun EngineEntity.addCommonPlayerComponents(
  world: World,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  name: String,
  id: String,
  killableComponent: KillableComponent?,
  wantedFamilies: Array<Pair<Family, String>>,
  whenReady: (Entity) -> Unit
) {
  with(WorldComponent(world))
  with(IdComponent(id))
  with(PositionComponent(worldX, worldY))

  // BASIC_DYNAMIC_ENTITY_ARRAY
  with(VelocityComponent(dx, dy))
  with<GroundedComponent>()

  // player family
  with(NamedComponent(name))

  with(killableComponent ?: KillableComponent())
  with(
    InventoryComponent(Material.entries.size).also {
      for (material in Material.entries) {
        it += Item(material, 1_000_000u)
      }
    }
  )

  createBody2DBodyComponent(entity, world, worldX, worldY, dx, dy, PLAYER_WIDTH, PLAYER_HEIGHT, wantedFamilies, afterBodyCreated = whenReady) {
    createPlayerFixture(it)
    createSecondaryPlayerFixture(it, PLAYERS_FOOT_USER_DATA, width = PLAYER_WIDTH / 3f, height = ESSENTIALLY_ZERO, ry = -(PLAYER_HEIGHT / 2f))
    createPlayerTouchAreaFixture(it, PLAYERS_LEFT_ARM_USER_DATA, -1)
    createPlayerTouchAreaFixture(it, PLAYERS_RIGHT_ARM_USER_DATA, 1)
  }
}

private fun EngineEntity.addCommonClientPlayerComponents(world: ClientWorld, controlled: Boolean) {
  if (controlled) {
    // This entity will handle input events
    with(LocallyControlledComponent(KeyboardControls(world)))
    with<FollowedByCameraTag>()
    with<InputEventQueue>()
    with<SelectedInventoryItemComponent>()
  }

  with<PhysicsEventQueue>()
  with<LookDirectionComponent>()
  with(TextureRegionComponent(KAssets.playerTexture))
}

fun Engine.createMPServerPlayerEntity(world: ServerWorld, protoEntity: ProtoWorld.Entity, sharedInformation: SharedInformation): CompletableFuture<Entity> {
  val protoPlayer = protoEntity.playerOrNull ?: return CompletableFuture.failedFuture(IllegalStateException("Failed to find player component in entity protobuf"))
  val living = protoEntity.livingOrNull ?: return CompletableFuture.failedFuture(IllegalStateException("Failed to find living component in entity protobuf"))
  return createMPServerPlayerEntity(
    world,
    protoEntity.position.x,
    protoEntity.position.y,
    protoEntity.velocity.x,
    protoEntity.velocity.y,
    protoEntity.name,
    protoEntity.uuid,
    sharedInformation,
    KillableComponent(maxHealth = living.maxHealth, health = living.health)
  )
}

fun Engine.createMPClientPlayerEntity(world: ServerClientWorld, protoEntity: ProtoWorld.Entity, controlled: Boolean): CompletableFuture<Entity> {
  val protoPlayer = protoEntity.playerOrNull ?: return CompletableFuture.failedFuture(IllegalStateException("Failed to find player component in entity protobuf"))
  val living = protoEntity.livingOrNull ?: return CompletableFuture.failedFuture(IllegalStateException("Failed to find living component in entity protobuf"))
  return createMPClientPlayerEntity(
    world,
    protoEntity.position.x,
    protoEntity.position.y,
    protoEntity.velocity.x,
    protoEntity.velocity.y,
    protoEntity.name,
    protoEntity.uuid,
    controlled,
    KillableComponent(maxHealth = living.maxHealth, health = living.health)
  )
}

fun Engine.createMPServerPlayerEntity(
  world: ServerWorld,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  name: String,
  id: String,
  sharedInformation: SharedInformation,
  killableComponent: KillableComponent?
): CompletableFuture<Entity> {
  val completableFuture = CompletableFuture<Entity>()
  if (Main.isServer) {
    entity {
      with(SharedInformationComponent(sharedInformation))
      addCommonPlayerComponents(world, worldX, worldY, dx, dy, name, id, killableComponent, COMMON_PLAYER_FAMILIES) {
        completableFuture.complete(it)
      }
    }
  } else {
    completableFuture.completeExceptionally(IllegalStateException("Cannot create a server player when this is not a server instance"))
  }
  return completableFuture
}

fun Engine.createMPClientPlayerEntity(
  world: ClientWorld,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  name: String,
  id: String,
  controlled: Boolean,
  killableComponent: KillableComponent?
): CompletableFuture<Entity> {
  val completableFuture = CompletableFuture<Entity>()
//  if (Main.isServerClient) {
  entity {
    addCommonClientPlayerComponents(world, controlled)
    addCommonPlayerComponents(world, worldX, worldY, dx, dy, name, id, killableComponent, if (controlled) CONTROLLED_CLIENT_PLAYER_FAMILIES else CLIENT_PLAYER_FAMILIES) {
      it.box2d.disableGravity()
      completableFuture.complete(it)
    }
  }
//  } else {
//    completableFuture.completeExceptionally(IllegalStateException("Cannot create a server-client player when this is not a server-client instance"))
//  }
  return completableFuture
}

fun Engine.createSPPlayerEntity(
  world: SinglePlayerWorld,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  name: String = "Player",
  id: String? = null
): CompletableFuture<Entity> {
  val completableFuture = CompletableFuture<Entity>()
  if (Main.isSingleplayer) {
    entity {
      addCommonClientPlayerComponents(world, true)
      addCommonPlayerComponents(world, worldX, worldY, dx, dy, name, id ?: UUID.randomUUID().toString(), null, CONTROLLED_CLIENT_PLAYER_FAMILIES) {
        completableFuture.complete(it)
      }
    }
  } else {
    completableFuture.completeExceptionally(IllegalStateException("Cannot create a singleplayer player when this is not a singleplayer instance"))
  }
  return completableFuture
}
