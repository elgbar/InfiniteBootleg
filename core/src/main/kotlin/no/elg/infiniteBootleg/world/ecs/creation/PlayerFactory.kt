package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import no.elg.infiniteBootleg.items.InventoryElement.Companion.asProto
import no.elg.infiniteBootleg.items.Item.Companion.asProto
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.box2D
import no.elg.infiniteBootleg.protobuf.EntityKt.inventory
import no.elg.infiniteBootleg.protobuf.EntityKt.killable
import no.elg.infiniteBootleg.protobuf.EntityKt.locallyControlled
import no.elg.infiniteBootleg.protobuf.EntityKt.lookDirection
import no.elg.infiniteBootleg.protobuf.EntityKt.selectedItem
import no.elg.infiniteBootleg.protobuf.EntityKt.tags
import no.elg.infiniteBootleg.protobuf.EntityKt.texture
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType
import no.elg.infiniteBootleg.protobuf.entity
import no.elg.infiniteBootleg.protobuf.vector2f
import no.elg.infiniteBootleg.protobuf.vector2i
import no.elg.infiniteBootleg.util.INITIAL_BRUSH_SIZE
import no.elg.infiniteBootleg.util.INITIAL_INSTANT_BREAK
import no.elg.infiniteBootleg.util.INITIAL_INTERACT_RADIUS
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.Tool
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent
import no.elg.infiniteBootleg.world.ecs.components.InputEventQueueComponent.Companion.PROTO_INPUT_EVENT
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent.Companion.DEFAULT_MAX_HEALTH
import no.elg.infiniteBootleg.world.ecs.components.PhysicsEventQueueComponent.Companion.PROTO_PHYSICS_EVENT
import no.elg.infiniteBootleg.world.ecs.controlledEntityFamily
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithInputEventFamily
import no.elg.infiniteBootleg.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.world.ecs.followEntityFamily
import no.elg.infiniteBootleg.world.ecs.load
import no.elg.infiniteBootleg.world.ecs.localPlayerFamily
import no.elg.infiniteBootleg.world.ecs.playerFamily
import no.elg.infiniteBootleg.world.world.World
import java.util.UUID
import java.util.concurrent.CompletableFuture

const val PLAYER_WIDTH = 1.5f - 0.2f
const val PLAYER_HEIGHT = 3f - 0.2f
const val PLAYERS_FOOT_USER_DATA = "A bloody foot!"
const val HOLE_DETECTOR_USER_DATA = "down the drain"
const val PLAYERS_RIGHT_ARM_USER_DATA = "Righty"
const val PLAYERS_LEFT_ARM_USER_DATA = "Left hand"

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
  name: String,
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
  this.name = name
  grounded = GroundedComponent.PROTO_GROUNDED
  killable = killable {
    health = DEFAULT_MAX_HEALTH
    maxHealth = DEFAULT_MAX_HEALTH
  }
  inventory = inventory {
    maxSize = Material.entries.size + Tool.entries.size
    items += Material.entries.map { it.toItem().asProto() }
    items += Tool.entries.map { it.toItem().asProto() }
  }
  box2D = box2D {
    bodyType = ProtoWorld.Entity.Box2D.BodyType.PLAYER
  }
}

private fun EntityKt.Dsl.addCommonClientPlayerComponentsProto(controlled: Boolean) {
  if (controlled) {
    inputEvent = PROTO_INPUT_EVENT
    locallyControlled = locallyControlled {
      instantBreak = INITIAL_INSTANT_BREAK
      brushRadius = INITIAL_BRUSH_SIZE
      interactRadius = INITIAL_INTERACT_RADIUS
    }

    selectedItem = selectedItem {
      element = Tool.PICKAXE.asProto()
    }
  }
  physicsEvent = PROTO_PHYSICS_EVENT
  lookDirection = lookDirection {
    direction = vector2i {
      x = 0
      y = 0
    }
  }
  texture = texture {
    texture = Main.inst().assets.playerTexture.name
  }
}

fun World.createNewPlayer(): CompletableFuture<Entity> = load(createNewProtoPlayer())

fun World.createNewProtoPlayer(controlled: Boolean = Main.isSingleplayer, configure: EntityKt.Dsl.() -> Unit = {}): ProtoWorld.Entity =
  entity {
    val world = this@createNewProtoPlayer
    val (spawnX, spawnY) = world.spawn
    addCommonClientPlayerComponentsProto(controlled)
    addCommonPlayerComponentsProto(
      id = UUID.randomUUID().toString(),
      world = world,
      worldX = spawnX.toFloat(),
      worldY = spawnY.toFloat(),
      dx = 0f,
      dy = 0f,
      name = "Player",
      controlled = controlled
    )
    configure()
  }
