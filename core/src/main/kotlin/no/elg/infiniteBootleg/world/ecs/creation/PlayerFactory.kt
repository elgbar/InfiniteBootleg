package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import ktx.ashley.EngineEntity
import ktx.ashley.with
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.killableOrNull
import no.elg.infiniteBootleg.server.SharedInformation
import no.elg.infiniteBootleg.util.futureEntity
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent
import no.elg.infiniteBootleg.world.ecs.components.NameComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.GroundedComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.LocallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEventQueue
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag
import no.elg.infiniteBootleg.world.ecs.components.transients.SharedInformationComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.TransientTag
import no.elg.infiniteBootleg.world.ecs.controlledEntityFamily
import no.elg.infiniteBootleg.world.ecs.controlledEntityWithInputEventFamily
import no.elg.infiniteBootleg.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.world.ecs.followEntityFamily
import no.elg.infiniteBootleg.world.ecs.localPlayerFamily
import no.elg.infiniteBootleg.world.ecs.playerFamily
import no.elg.infiniteBootleg.world.world.ClientWorld
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import no.elg.infiniteBootleg.world.world.ServerWorld
import no.elg.infiniteBootleg.world.world.SinglePlayerWorld
import no.elg.infiniteBootleg.world.world.World
import java.util.UUID
import java.util.concurrent.CompletableFuture

const val PLAYER_WIDTH = 1.5f - 0.2f
const val PLAYER_HEIGHT = 3f - 0.2f
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
  future: CompletableFuture<Unit>,
  whenReady: (Entity) -> Unit = {}
) {
  withRequiredComponents(ProtoWorld.Entity.EntityType.PLAYER, world, worldX, worldY, id)

  // BASIC_DYNAMIC_ENTITY_ARRAY
  with(VelocityComponent(dx, dy))
  with<GroundedComponent>()
  with<TransientTag>()

  // player family
  with(NameComponent(name))

  with(killableComponent ?: KillableComponent())
  with(
    InventoryComponent(Material.entries.size).also {
      for (material in Material.entries) {
        it += Item(material, 1_000_000u)
      }
    }
  )
  createPlayerBodyComponent(world, worldX, worldY, dx, dy, wantedFamilies) {
    whenReady(it)
    future.complete(Unit)
  }
}

fun EngineEntity.addCommonClientPlayerComponents(controlled: Boolean) {
  if (controlled) {
    // This entity will handle input events
    with(LocallyControlledComponent())
    with<FollowedByCameraTag>()
    with<InputEventQueue>()
    with<SelectedInventoryItemComponent>()
  }

  with<PhysicsEventQueue>()
  with<LookDirectionComponent>()
  with(TextureRegionComponent(KAssets.playerTexture))
}

fun Engine.createMPServerPlayerEntity(world: ServerWorld, protoEntity: ProtoWorld.Entity, sharedInformation: SharedInformation): CompletableFuture<Entity> {
  if (!protoEntity.hasPlayer()) return CompletableFuture.failedFuture(IllegalStateException("Failed to find player component in entity protobuf"))
  val living = protoEntity.killableOrNull ?: return CompletableFuture.failedFuture(IllegalStateException("Failed to find living component in entity protobuf"))
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
  if (!protoEntity.hasPlayer()) return CompletableFuture.failedFuture(IllegalStateException("Failed to find player component in entity protobuf"))
  val living = protoEntity.killableOrNull ?: return CompletableFuture.failedFuture(IllegalStateException("Failed to find living component in entity protobuf"))
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
  worldX: Number,
  worldY: Number,
  dx: Float,
  dy: Float,
  name: String,
  id: String,
  sharedInformation: SharedInformation,
  killableComponent: KillableComponent?
): CompletableFuture<Entity> {
  return if (Main.isServer) {
    futureEntity { future ->
      with(SharedInformationComponent(sharedInformation))
      addCommonPlayerComponents(world, worldX.toFloat(), worldY.toFloat(), dx, dy, name, id, killableComponent, COMMON_PLAYER_FAMILIES, future)
    }
  } else {
    CompletableFuture.failedFuture(IllegalStateException("Cannot create a server player when this is not a server instance"))
  }
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
): CompletableFuture<Entity> =
  futureEntity { future ->
    addCommonClientPlayerComponents(controlled)
    addCommonPlayerComponents(world, worldX, worldY, dx, dy, name, id, killableComponent, if (controlled) CONTROLLED_CLIENT_PLAYER_FAMILIES else CLIENT_PLAYER_FAMILIES, future) {
      it.box2d.disableGravity()
    }
  }

fun Engine.createSPPlayerEntity(
  world: SinglePlayerWorld,
  worldX: Number,
  worldY: Number,
  dx: Float = 0f,
  dy: Float = 0f,
  name: String = "Player",
  id: String? = null
): CompletableFuture<Entity> =
  if (Main.isSingleplayer) {
    futureEntity { future ->
      addCommonClientPlayerComponents(true)
      addCommonPlayerComponents(
        world,
        worldX.toFloat(),
        worldY.toFloat() + PLAYER_HEIGHT,
        dx,
        dy,
        name,
        id ?: UUID.randomUUID().toString(),
        null,
        CONTROLLED_CLIENT_PLAYER_FAMILIES,
        future
      )
    }
  } else {
    CompletableFuture.failedFuture(IllegalStateException("Cannot create a singleplayer player when this is not a singleplayer instance"))
  }
