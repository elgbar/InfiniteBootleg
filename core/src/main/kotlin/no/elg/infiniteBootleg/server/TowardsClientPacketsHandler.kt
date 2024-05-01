package no.elg.infiniteBootleg.server

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.console.logPacket
import no.elg.infiniteBootleg.events.InitialChunksOfWorldLoadedEvent
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.inventory.container.OwnedContainer.Companion.fromProto
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.ContainerUpdate
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity
import no.elg.infiniteBootleg.protobuf.Packets.Disconnect
import no.elg.infiniteBootleg.protobuf.Packets.MoveEntity
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_DESPAWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_INITIAL_CHUNKS_SENT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_LOGIN_STATUS
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_SPAWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_START_GAME
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_UPDATE_CHUNK
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BLOCK_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BREAKING_BLOCK
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_CONTAINER_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_DISCONNECT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_HEARTBEAT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_MOVE_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_SECRET_EXCHANGE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_WORLD_SETTINGS
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CLIENT_WORLD_LOADED
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.UNRECOGNIZED
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.protobuf.Packets.StartGame
import no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock
import no.elg.infiniteBootleg.protobuf.Packets.UpdateChunk
import no.elg.infiniteBootleg.protobuf.Packets.WorldSettings
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType.FALLING_BLOCK
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType.PLAYER
import no.elg.infiniteBootleg.protobuf.breakingBlockOrNull
import no.elg.infiniteBootleg.protobuf.containerUpdateOrNull
import no.elg.infiniteBootleg.protobuf.despawnEntityOrNull
import no.elg.infiniteBootleg.protobuf.disconnectOrNull
import no.elg.infiniteBootleg.protobuf.lookDirectionOrNull
import no.elg.infiniteBootleg.protobuf.moveEntityOrNull
import no.elg.infiniteBootleg.protobuf.secretExchangeOrNull
import no.elg.infiniteBootleg.protobuf.serverLoginStatusOrNull
import no.elg.infiniteBootleg.protobuf.spawnEntityOrNull
import no.elg.infiniteBootleg.protobuf.startGameOrNull
import no.elg.infiniteBootleg.protobuf.updateBlockOrNull
import no.elg.infiniteBootleg.protobuf.updateChunkOrNull
import no.elg.infiniteBootleg.protobuf.worldSettingsOrNull
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.server.ClientBoundHandler.Companion.TAG
import no.elg.infiniteBootleg.server.SharedInformation.Companion.HEARTBEAT_PERIOD_MS
import no.elg.infiniteBootleg.util.toCompact
import no.elg.infiniteBootleg.util.toVector2
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.util.worldXYtoChunkCompactLoc
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.creation.createFallingBlockStandaloneEntity
import no.elg.infiniteBootleg.world.ecs.load
import no.elg.infiniteBootleg.world.managers.container.ServerClientWorldContainerManager
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Handle packets sent FROM the server THIS client, will quietly drop any packets that are malformed
 *
 * This method will be called on the netty i/o thread, so heavy operations (i.e., setting a block) should be executed explicitly on async/main thread
 *
 * @author Elg
 */
fun ServerClient.handleClientBoundPackets(packet: Packets.Packet) {
  logPacket("client<-server", packet)
  when (packet.type) {
    // Gameplay related packets
    DX_HEARTBEAT -> if (packet.hasHeartbeat()) handleHeartbeat()
    DX_MOVE_ENTITY -> packet.moveEntityOrNull?.let { scheduler.executeAsync { asyncHandleMoveEntity(it) } }
    DX_BLOCK_UPDATE -> packet.updateBlockOrNull?.let { scheduler.executeAsync { asyncHandleBlockUpdate(it) } }
    CB_SPAWN_ENTITY -> packet.spawnEntityOrNull?.let { scheduler.executeAsync { asyncHandleSpawnEntity(it) } }
    CB_UPDATE_CHUNK -> packet.updateChunkOrNull?.let { scheduler.executeAsync { asyncHandleUpdateChunk(it) } }
    CB_DESPAWN_ENTITY -> packet.despawnEntityOrNull?.let { scheduler.executeAsync { asyncHandleDespawnEntity(it) } }
    DX_BREAKING_BLOCK -> packet.breakingBlockOrNull?.let { scheduler.executeAsync { asyncHandleBreakingBlock(it) } }
    DX_CONTAINER_UPDATE -> packet.containerUpdateOrNull?.let { scheduler.executeAsync { asyncHandleContainerUpdate(it) } }

    // Login related packets
    DX_SECRET_EXCHANGE -> packet.secretExchangeOrNull?.let { handleSecretExchange(it) }
    CB_START_GAME -> packet.startGameOrNull?.let { handleStartGame(it) }
    CB_LOGIN_STATUS -> packet.serverLoginStatusOrNull?.let { handleLoginStatus(it) }
    CB_INITIAL_CHUNKS_SENT -> handleInitialChunkSent()

    // Misc packets
    DX_DISCONNECT -> handleDisconnect(packet.disconnectOrNull)
    DX_WORLD_SETTINGS -> packet.worldSettingsOrNull?.let { handleWorldSettings(it) }

    // Error handling
    UNRECOGNIZED -> ctx.fatal("Unknown packet type received by client: ${packet.type}")
    else -> ctx.fatal("Client cannot handle packet of type ${packet.type}")
  }
}

// ///////////////////////
// NOT SYNCED HANDLERS  //
// ///////////////////////

private fun ServerClient.setupHeartbeat() {
  val sharedInformation = sharedInformation!!
  sharedInformation.heartbeatTask = ctx.executor().scheduleAtFixedRate({
    sendServerBoundPacket(serverBoundHeartbeat())
    if (sharedInformation.lostConnection()) {
      ctx.fatal("Server stopped responding, heartbeats not received")
    }
  }, HEARTBEAT_PERIOD_MS, HEARTBEAT_PERIOD_MS, TimeUnit.MILLISECONDS)
}

private fun ServerClient.handleHeartbeat() {
  sharedInformation?.beat() ?: Main.logger().error("handleHeartbeat", "Failed to beat, because of null shared information")
}

private fun ServerClient.handleDisconnect(disconnect: Disconnect?) {
  ConnectingScreen.info = "Disconnected: ${disconnect?.reason ?: "Unknown reason"}"
  ctx.close()
}

private fun ServerClient.handleWorldSettings(worldSettings: WorldSettings) {
  val world = this.world
  if (world == null) {
    Main.logger().warn("handleWorldSettings", "Failed to find world")
    return
  }
  Main.logger().debug("handleWorldSettings", "spawn? ${worldSettings.hasSpawn()}, time? ${worldSettings.hasTime()}, time scale? ${worldSettings.hasTimeScale()}")
  if (worldSettings.hasSpawn()) {
    world.spawn = worldSettings.spawn.toCompact()
  }
  if (worldSettings.hasTime()) {
    world.worldTime.time = worldSettings.time
  }
  if (worldSettings.hasTimeScale()) {
    world.worldTime.timeScale = worldSettings.timeScale
  }
}

// Login packets, in order of receiving

private fun ServerClient.handleSecretExchange(secretExchange: SecretExchange) {
  val uuid = secretExchange.entityUUID
  val sharedInformation = SharedInformation(uuid, secretExchange.secret)
  this.sharedInformation = sharedInformation
  Main.logger().debug("LOGIN", "Secret received from sever sending response")
  sendServerBoundPacket(serverBoundClientSecretResponse(sharedInformation))
}

private fun ServerClient.handleLoginStatus(loginStatus: ServerLoginStatus) {
  when (loginStatus.status) {
    ServerLoginStatus.ServerStatus.ALREADY_LOGGED_IN -> {
      ctx.fatal("You are already logged in!")
      return
    }

    ServerLoginStatus.ServerStatus.FULL_SERVER -> {
      ctx.fatal("Server is full")
      return
    }

    ServerLoginStatus.ServerStatus.PROCEED_LOGIN -> {
      Main.logger().debug("LOGIN", "User accepted by server, logging in...")
      ConnectingScreen.info = "Logging in..."
    }

    ServerLoginStatus.ServerStatus.LOGIN_SUCCESS -> handleLoginSuccess()

    ServerLoginStatus.ServerStatus.UNRECOGNIZED, null -> {
      ctx.fatal("Unrecognized server status")
      return
    }
  }
}

private fun ServerClient.handleLoginSuccess() {
  val world = world
  if (world == null) {
    ctx.fatal("Failed to get world")
    return
  }

  val protoPlayerEntity = protoEntity
  if (protoPlayerEntity == null) {
    ctx.fatal("Invalid player client side: Did not a receive an entity to control")
    return
  }
  val futurePlayer: CompletableFuture<Entity> = world.load(protoPlayerEntity).orTimeout(10, TimeUnit.SECONDS)
  ConnectingScreen.info = "Login successful! Waiting for player to be spawned..."

  futurePlayer.whenCompleteAsync { player, e ->
    if (e != null) {
      ctx.fatal("Invalid player client side ${e::class.simpleName}: ${e.message}")
      return@whenCompleteAsync
    } else {
      player.box2d.enableGravity()
      Main.logger().debug("handleSpawnEntity", "Server sent the entity to control")

      Main.inst().scheduler.executeSync {
        started = true
        ClientMain.inst().screen = WorldScreen(world, false)
        dispatchEvent(WorldLoadedEvent(world)) // must be after setting the world screen for the event to be listened to
        setupHeartbeat()
        Main.logger().debug("LOGIN", "Logged into server successfully")
      }
    }
  }
}

private fun ServerClient.handleStartGame(startGame: StartGame) {
  Main.logger().debug("LOGIN", "Initialization okay, loading world")
  scheduler.executeSync {
    val protoWorld = startGame.world
    this.world = ServerClientWorld(protoWorld, this).apply {
      loadFromProtoWorld(protoWorld)
      worldTicker.start()
    }

    if (startGame.controlling.entityType != PLAYER) {
      ctx.fatal("Can only control a player, got ${startGame.controlling.entityType}")
    } else {
      this.protoEntity = startGame.controlling
      Main.logger().debug("LOGIN", "World loaded, waiting for chunks")
      sendServerBoundPacket(serverBoundPacketBuilder(SB_CLIENT_WORLD_LOADED).build())
    }
  }
}

private fun ServerClient.handleInitialChunkSent() {
  Main.logger().debug("CB_INITIAL_CHUNKS_SENT", "Setting chunks as loaded")
  chunksLoaded = true
  val world = world ?: run {
    ctx.fatal("Failed to find world")
    return
  }
  dispatchEvent(InitialChunksOfWorldLoadedEvent(world))
}

// //////////////////
// ASYNC HANDLERS  //
// //////////////////

private fun ServerClient.asyncHandleBlockUpdate(blockUpdate: UpdateBlock) {
  if (!chunksLoaded) {
    return
  }
  val world = this.world
  if (world == null) {
    Main.logger().warn("handleBlockUpdate", "Failed to find world")
    return
  }
  val worldX = blockUpdate.pos.x
  val worldY = blockUpdate.pos.y
  if (world.isChunkLoaded(worldXYtoChunkCompactLoc(worldX, worldY))) {
    val protoBlock = if (blockUpdate.hasBlock()) blockUpdate.block else null
    world.setBlock(worldX, worldY, protoBlock, updateTexture = false, prioritize = false, sendUpdatePacket = false)
  } else {
    Main.logger().warn("handleSpawnEntity", "Sever sent block update to unloaded client chunk")
    sendServerBoundPacket(serverBoundChunkRequestPacket(blockUpdate.pos))
  }
}

private fun ServerClient.asyncHandleSpawnEntity(spawnEntity: Packets.SpawnEntity) {
  if (chunksLoaded) {
    Main.logger().debug("handleSpawnEntity") { "Server sent spawn entity packet\n${spawnEntity.entity}" }
  } else {
    Main.logger().debug("handleSpawnEntity") { "Server sent spawn entity packet before chunks loaded packet, ignoring it.\n${spawnEntity.entity}" }
    return
  }
  val world = this.world
  if (world == null) {
    Main.logger().warn("handleSpawnEntity", "Failed to find world")
    return
  }
  val position = spawnEntity.entity.position
  val chunkPosX = position.x.worldToChunk()
  val chunkPosY = position.y.worldToChunk()
  val chunk = world.getChunk(chunkPosX, chunkPosY, true)
  if (chunk == null) {
    Main.logger().warn("handleSpawnEntity", "Server sent spawn entity in unloaded chunk $chunkPosX, $chunkPosY")
    return
  }
  Main.logger().debug("handleSpawnEntity") { "Spawning a ${spawnEntity.entity.entityType}" }

  when (spawnEntity.entity.entityType) {
    PLAYER -> world.load(spawnEntity.entity)
    FALLING_BLOCK -> world.engine.createFallingBlockStandaloneEntity(world, spawnEntity.entity)
    else -> Main.logger().error("Cannot spawn a ${spawnEntity.entity.entityType} yet")
  }
}

private fun ServerClient.asyncHandleUpdateChunk(updateChunk: UpdateChunk) {
  val entities = updateChunk.chunk.entitiesCount
  if (entities > 0) {
    Main.logger().warn(TAG, "Got $entities entities in chunk update")
  }
  val world = world
  if (world == null) {
    ctx.fatal("Tried to update chunk before world was sent")
    return
  }
  val chunk = world.chunkLoader.loadChunkFromProto(updateChunk.chunk)
  if (chunk == null) {
    Main.logger().warn(TAG, "Failed to load the chunk from proto")
    return
  }
  world.updateChunk(chunk, false)
}

private fun ServerClient.asyncHandleMoveEntity(moveEntity: MoveEntity) {
  if (!started) {
    return
  }
  val world = world
  if (world == null) {
    Main.logger().warn("handleMoveEntity", "Failed to find world")
    return
  }
  val chunkLoc = worldXYtoChunkCompactLoc(moveEntity.position.x.toInt(), moveEntity.position.y.toInt())
  if (!world.isChunkLoaded(chunkLoc)) {
    Main.logger().warn("Server sent move entity packet to unloaded chunk")
    return
  }

  val uuid = moveEntity.uuid
  if (uuid == null) {
    ctx.fatal("UUID cannot be parsed '${moveEntity.uuid}'")
    return
  }
  val entity = world.getEntity(uuid)
  if (entity == null) {
    Main.logger().warn("Cannot move unknown entity '${moveEntity.uuid}'")
    sendServerBoundPacket(serverBoundEntityRequest(uuid))
    return
  }
  if (Main.inst().isAuthorizedToChange(entity)) {
    val clientPos = entity.position
    val serverPos = moveEntity.position.toVector2()

    val clientServerDiffSquaredToUpdateControllingEntity = 2
    if (clientPos.dst2(serverPos) <= clientServerDiffSquaredToUpdateControllingEntity) {
      // If we're less then 2 blocks away from the server, we don't need to move
      return
    }
  }
  entity.teleport(moveEntity.position.x, moveEntity.position.y)
  entity.setVelocity(moveEntity.velocity.x, moveEntity.velocity.y)

  // Only set look direction if it exists on the entity from before
  moveEntity.lookDirectionOrNull?.let { entity.lookDirectionComponentOrNull?.direction = Direction.valueOf(it) }
}

/**
 * List of despawn reasons that should not be logged as a warning
 */
private val nonWarnDespawnReasons = listOf(DespawnEntity.DespawnReason.UNKNOWN_ENTITY, DespawnEntity.DespawnReason.CHUNK_UNLOADED)

private fun ServerClient.asyncHandleDespawnEntity(despawnEntity: DespawnEntity) {
  val world = world
  if (world == null) {
    Main.logger().error("handleDespawnEntity", "Failed to find world")
    return
  }
  val uuid: String = despawnEntity.uuid
  val entity: Entity = world.getEntity(uuid) ?: run {
    if (despawnEntity.despawnReason !in nonWarnDespawnReasons) {
      Main.logger().warn("handleDespawnEntity", "Failed to despawn unknown entity with uuid '${despawnEntity.uuid}', reason ${despawnEntity.despawnReason}")
    }
    return
  }
  Main.logger().debug("handleDespawnEntity", "Despawning entity ${despawnEntity.uuid} with reason ${despawnEntity.despawnReason}")
  world.removeEntity(entity)
}

private fun ServerClient.asyncHandleBreakingBlock(breakingBlock: Packets.BreakingBlock) {
  for (breakingProgress in breakingBlock.breakingProgressList) {
    breakingBlockCache.put(breakingProgress.blockLocation.toCompact(), breakingProgress.progress)
  }
}

private fun ServerClient.asyncHandleContainerUpdate(containerUpdate: ContainerUpdate) {
  val containerManager = world?.worldContainerManager as ServerClientWorldContainerManager
  val ownedContainer = containerUpdate.worldContainer.fromProto()
  containerManager.updateContainerFromServer(ownedContainer)
}
