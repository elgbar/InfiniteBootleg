package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import no.elg.infiniteBootleg.core.assets.InfAssets
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.INITIAL_BRUSH_SIZE
import no.elg.infiniteBootleg.core.util.INITIAL_INSTANT_BREAK
import no.elg.infiniteBootleg.core.util.INITIAL_INTERACT_RADIUS
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.InputEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.controlledEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.controlledEntityWithInputEventFamily
import no.elg.infiniteBootleg.core.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.core.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.core.world.ecs.followEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.load
import no.elg.infiniteBootleg.core.world.ecs.localPlayerFamily
import no.elg.infiniteBootleg.core.world.ecs.playerFamily
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.box2D
import no.elg.infiniteBootleg.protobuf.EntityKt.hotbar
import no.elg.infiniteBootleg.protobuf.EntityKt.killable
import no.elg.infiniteBootleg.protobuf.EntityKt.locallyControlled
import no.elg.infiniteBootleg.protobuf.EntityKt.lookDirection
import no.elg.infiniteBootleg.protobuf.EntityKt.tags
import no.elg.infiniteBootleg.protobuf.EntityKt.texture
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType
import no.elg.infiniteBootleg.protobuf.container
import no.elg.infiniteBootleg.protobuf.containerOwner
import no.elg.infiniteBootleg.protobuf.entity
import no.elg.infiniteBootleg.protobuf.ownedContainer
import no.elg.infiniteBootleg.protobuf.vector2f
import no.elg.infiniteBootleg.protobuf.vector2i
import java.util.UUID
import java.util.concurrent.CompletableFuture

const val PLAYER_WIDTH = 1.5f - 0.2f
const val PLAYER_HEIGHT = 3f - 0.2f
const val PLAYERS_FOOT_USER_DATA = "A bloody foot!"
const val HOLE_DETECTOR_USER_DATA = "down the drain"
const val PLAYERS_EAST_ARM_USER_DATA = "Righty"
const val PLAYERS_WEST_ARM_USER_DATA = "Left hand"

val COMMON_PLAYER_FAMILIES: Array<Pair<Family, String>> = arrayOf(
  playerFamily to "playerFamily",
  basicDynamicEntityFamily to "basicDynamicEntityFamily"
)
val NON_CONTROLLED_PLAYER_FAMILIES: Array<Pair<Family, String>> = arrayOf(
  *COMMON_PLAYER_FAMILIES,
  drawableEntitiesFamily to "drawableEntitiesFamily",
  entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily"
)
val CONTROLLED_CLIENT_PLAYER_FAMILIES: Array<Pair<Family, String>> = arrayOf(
  *NON_CONTROLLED_PLAYER_FAMILIES,
  drawableEntitiesFamily to "drawableEntitiesFamily",
  localPlayerFamily to "localPlayerFamily",
  controlledEntityFamily to "controlledEntityFamily",
  followEntityFamily to "followEntityFamily",
  entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily",
  controlledEntityWithInputEventFamily to "controlledEntityWithInputEventFamily"
)

private fun EntityKt.Dsl.addCommonPlayerComponentsProto(
  id: String,
  world: World,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  playerName: String,
  controlled: Boolean
) {
  withRequiredComponents(EntityType.PLAYER, world, worldX, worldY, id)
  tags = tags {
    transient = true
    canBeOutOfBounds = true
    followedByCamera = controlled
  }
  velocity = vector2f {
    x = dx
    y = dy
  }
  name = playerName
  grounded = ProtoWorld.Entity.Grounded.getDefaultInstance()
  killable = killable {
    health = KillableComponent.DEFAULT_MAX_HEALTH
    maxHealth = KillableComponent.DEFAULT_MAX_HEALTH
  }
  ownedContainer = ownedContainer {
    owner = containerOwner { entityOwner = this@addCommonPlayerComponentsProto.ref }
    container = container {
      maxSize = 40
      name = "Inventory"
    }
  }
  hotbar = hotbar {
    selected = 0
  }
  box2D = box2D {
    bodyType = ProtoWorld.Entity.Box2D.BodyType.PLAYER
  }
}

private fun EntityKt.Dsl.addCommonClientPlayerComponentsProto(controlled: Boolean) {
  if (controlled) {
    inputEvent = InputEventQueueComponent.PROTO_INPUT_EVENT
    locallyControlled = locallyControlled {
      instantBreak = INITIAL_INSTANT_BREAK
      brushRadius = INITIAL_BRUSH_SIZE
      interactRadius = INITIAL_INTERACT_RADIUS
    }
  }
  physicsEvent = PhysicsEventQueueComponent.PROTO_PHYSICS_EVENT
  lookDirection = lookDirection {
    direction = vector2i {
      x = 0
      y = 0
    }
  }
  texture = texture {
    texture = InfAssets.PLAYER_TEXTURE
  }
}

fun World.createNewPlayer(): CompletableFuture<Entity> = load(createNewProtoPlayer())

fun World.createNewProtoPlayer(controlled: Boolean = Main.isSingleplayer, configure: EntityKt.Dsl.() -> Unit = {}): ProtoWorld.Entity =
  entity {
    val world = this@createNewProtoPlayer
    val (spawnX, spawnY) = world.spawn
    addCommonPlayerComponentsProto(
      id = UUID.randomUUID().toString(),
      world = world,
      worldX = spawnX.toFloat(),
      worldY = spawnY.toFloat(),
      dx = 0f,
      dy = 0f,
      playerName = "Player",
      controlled = controlled
    )
    addCommonClientPlayerComponentsProto(controlled)
    configure()
  }
