package no.elg.infiniteBootleg.client.net

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.client.console.clientSideClientBoundMarker
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.ConnectingScreen
import no.elg.infiniteBootleg.client.screens.WorldScreen
import no.elg.infiniteBootleg.client.world.managers.container.ServerClientWorldContainerManager
import no.elg.infiniteBootleg.client.world.world.ServerClientWorld
import no.elg.infiniteBootleg.core.console.logPacket
import no.elg.infiniteBootleg.core.events.InitialChunksOfWorldLoadedEvent
import no.elg.infiniteBootleg.core.events.WorldLoadedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer.Companion.fromProto
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.net.ServerClient
import no.elg.infiniteBootleg.core.net.SharedInformation
import no.elg.infiniteBootleg.core.net.SharedInformation.Companion.HEARTBEAT_PERIOD_MS
import no.elg.infiniteBootleg.core.net.serverBoundChunkRequestPacket
import no.elg.infiniteBootleg.core.net.serverBoundClientSecretResponse
import no.elg.infiniteBootleg.core.net.serverBoundEntityRequest
import no.elg.infiniteBootleg.core.net.serverBoundHeartbeat
import no.elg.infiniteBootleg.core.net.serverBoundPacketBuilder
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.util.toCompact
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.util.worldXYtoChunkCompactLoc
import no.elg.infiniteBootleg.core.world.ContainerElement.Companion.fromProto
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.core.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.core.world.ecs.components.transients.RemoteEntityHoldingElement
import no.elg.infiniteBootleg.core.world.ecs.components.transients.RemoteEntityHoldingElement.Companion.remoteEntityHoldingElementComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.creation.createFallingBlockStandaloneEntity
import no.elg.infiniteBootleg.core.world.ecs.load
import no.elg.infiniteBootleg.core.world.ecs.system.WriteBox2DStateSystem
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.ContainerUpdate
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity
import no.elg.infiniteBootleg.protobuf.Packets.Disconnect
import no.elg.infiniteBootleg.protobuf.Packets.MoveEntity
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_DESPAWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_HOLDING_ITEM
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
import no.elg.infiniteBootleg.protobuf.holdingItemOrNull
import no.elg.infiniteBootleg.protobuf.lookDirectionOrNull
import no.elg.infiniteBootleg.protobuf.moveEntityOrNull
import no.elg.infiniteBootleg.protobuf.secretExchangeOrNull
import no.elg.infiniteBootleg.protobuf.serverLoginStatusOrNull
import no.elg.infiniteBootleg.protobuf.spawnEntityOrNull
import no.elg.infiniteBootleg.protobuf.startGameOrNull
import no.elg.infiniteBootleg.protobuf.updateBlockOrNull
import no.elg.infiniteBootleg.protobuf.updateChunkOrNull
import no.elg.infiniteBootleg.protobuf.worldSettingsOrNull
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Handle packets sent FROM the server THIS client, will quietly drop any packets that are malformed
 *
 * This method will be called on the netty i/o thread, so heavy operations (i.e., setting a block) should be executed explicitly on async/main thread
 *
 * @author Elg
 */
fun ServerClient.handleClientBoundPackets(packet: Packets.Packet) {
  logPacket(clientSideClientBoundMarker, packet)
  when (packet.type) {
    // Gameplay related packets
    DX_HEARTBEAT -> if (packet.hasHeartbeat()) handleHeartbeat()
    DX_MOVE_ENTITY -> packet.moveEntityOrNull?.let { launchOnAsyncSuspendable { asyncHandleMoveEntity(it) } }
    DX_BLOCK_UPDATE -> packet.updateBlockOrNull?.let { launchOnAsyncSuspendable { asyncHandleBlockUpdate(it) } }
    CB_SPAWN_ENTITY -> packet.spawnEntityOrNull?.let { launchOnAsyncSuspendable { asyncHandleSpawnEntity(it) } }
    CB_UPDATE_CHUNK -> packet.updateChunkOrNull?.let { launchOnAsyncSuspendable { asyncHandleUpdateChunk(it) } }
    CB_DESPAWN_ENTITY -> packet.despawnEntityOrNull?.let { launchOnAsyncSuspendable { asyncHandleDespawnEntity(it) } }
    DX_BREAKING_BLOCK -> packet.breakingBlockOrNull?.let { launchOnAsyncSuspendable { asyncHandleBreakingBlock(it) } }
    DX_CONTAINER_UPDATE -> packet.containerUpdateOrNull?.let { launchOnAsyncSuspendable { asyncHandleContainerUpdate(it) } }
    CB_HOLDING_ITEM -> packet.holdingItemOrNull?.let { launchOnAsyncSuspendable { asyncHandleHoldingItem(it) } }

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
    else -> {
      if (packet.direction == Packets.Packet.Direction.SERVER || packet.type.name.startsWith("SB_")) {
        ctx.fatal("Client got a server packet ${packet.type} direction ${packet.direction}")
      } else {
        ctx.fatal("Client cannot handle packet of type ${packet.type}")
      }
    }
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
  sharedInformation?.beat() ?: logger.error { "Failed to beat, because of null shared information" }
}

private fun ServerClient.handleDisconnect(disconnect: Disconnect?) {
  ConnectingScreen.info = "Disconnected: ${disconnect?.reason ?: "Unknown reason"}"
  ctx.close()
}

private fun ServerClient.handleWorldSettings(worldSettings: WorldSettings) {
  logger.debug { "spawn? ${worldSettings.hasSpawn()}, time? ${worldSettings.hasTime()}, time scale? ${worldSettings.hasTimeScale()}" }
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
  val id = secretExchange.ref.id
  if (id.isEmpty() || secretExchange.secret.isEmpty()) {
    ctx.fatal("Entity id nor secret can be empty")
    return
  }
  val sharedInformation = SharedInformation(id, secretExchange.secret)
  this.sharedInformation = sharedInformation
  logger.debug { "Secret received from sever sending response" }
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
      logger.debug { "User accepted by server, logging in..." }
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
  val world = clientWorld
  val protoPlayerEntity = protoEntity
  if (protoPlayerEntity == null) {
    ctx.fatal("Invalid player client side: Did not a receive an entity to control")
    return
  }
  val futurePlayer: CompletableFuture<Entity> = world.load(protoPlayerEntity).orTimeout(5, TimeUnit.SECONDS)
  ConnectingScreen.info = "Login successful! Waiting for player to be spawned..."

  futurePlayer.whenCompleteAsync { player, e ->
    if (e != null) {
      ctx.fatal("Invalid player client side ${e::class.simpleName}: ${e.message}")
      return@whenCompleteAsync
    } else {
      player.box2d.enableGravity()
      logger.debug { "Server sent the entity to control" }

      launchOnMainSuspendable {
        started = true
        ClientMain.inst().screen = WorldScreen(world, false)
        dispatchEvent(WorldLoadedEvent(world)) // must be after setting the world screen for the event to be listened to
        setupHeartbeat()
        logger.debug { "Logged into server successfully" }
      }
    }
  }
}

private fun ServerClient.handleStartGame(startGame: StartGame) {
  logger.debug { "Initialization okay, loading world" }
  launchOnMainSuspendable {
    if (startGame.controlling.entityType != PLAYER) {
      ctx.fatal("Can only control a player, got ${startGame.controlling.entityType}")
    }
    this@handleStartGame.protoEntity = startGame.controlling
    val protoWorld = startGame.world
    this@handleStartGame.worldOrNull = ServerClientWorld(protoWorld, this@handleStartGame).apply {
      val pos = startGame.controlling.position
      render.lookAt(pos.x, pos.y) // set position to not request wrong initial chunks
      loadFromProtoWorld(protoWorld)
      worldTicker.start()
    }
    logger.debug { "World loaded, waiting for chunks" }
    sendServerBoundPacket(serverBoundPacketBuilder(SB_CLIENT_WORLD_LOADED).build())
  }
}

private fun ServerClient.handleInitialChunkSent() {
  logger.debug { "Setting chunks as loaded" }
  val world = this.world // will call fatal if world is null
  chunksLoaded = true
  dispatchEvent(InitialChunksOfWorldLoadedEvent(world))
}

// //////////////////
// ASYNC HANDLERS  //
// //////////////////

private fun ServerClient.asyncHandleBlockUpdate(blockUpdate: UpdateBlock) {
  if (!chunksLoaded) {
    return
  }
  val worldX = blockUpdate.pos.x
  val worldY = blockUpdate.pos.y
  if (world.isChunkLoaded(worldXYtoChunkCompactLoc(worldX, worldY))) {
    val protoBlock = if (blockUpdate.hasBlock()) blockUpdate.block else null
    world.setBlock(worldX, worldY, protoBlock, updateTexture = true, prioritize = false, sendUpdatePacket = false)
  } else {
    logger.warn { "Sever sent block update to unloaded client chunk" }
    sendServerBoundPacket(serverBoundChunkRequestPacket(blockUpdate.pos))
  }
}

private fun ServerClient.asyncHandleSpawnEntity(spawnEntity: Packets.SpawnEntity) {
  if (chunksLoaded) {
    logger.debug { "Server sent spawn entity packet" }
  } else {
    logger.debug { "Server sent spawn entity packet before chunks loaded packet, ignoring it" }
    return
  }
  val protoEntity = spawnEntity.entity
  val position = protoEntity.position
  val chunkPosX = position.x.worldToChunk()
  val chunkPosY = position.y.worldToChunk()
  val chunk = world.getChunk(chunkPosX, chunkPosY, true)
  if (chunk == null) {
    logger.warn { "Server sent spawn entity in unloaded chunk $chunkPosX, $chunkPosY" }
    return
  }
  logger.debug { "Spawning a ${protoEntity.entityType}" }

  when (protoEntity.entityType) {
    PLAYER -> world.load(protoEntity)
    FALLING_BLOCK -> world.engine.createFallingBlockStandaloneEntity(world, protoEntity)
//    BLOCK -> {
//      val material = protoEntity.materialOrNull?.fromProto() ?: return
//      val localPosX = position.x.toInt().chunkOffset()
//      val localPosY = position.y.toInt().chunkOffset()
//      material.createBlock(world, chunk, localPosX, localPosY, protoEntity)
//    }

    else -> logger.error { "Cannot spawn a ${protoEntity.entityType} yet" }
  }
}

private fun ServerClient.asyncHandleUpdateChunk(updateChunk: UpdateChunk) {
  val entities = updateChunk.chunk.entitiesCount
  if (entities > 0) {
    logger.warn { "Got $entities entities in chunk update" }
  }
  val chunk = world.chunkLoader.loadChunkFromProto(updateChunk.chunk)
  if (chunk == null) {
    logger.warn { "Failed to load the chunk from proto" }
    return
  }
  world.updateChunk(chunk, newlyGenerated = false)
}

private const val CLIENT_SERVER_DIFF_SQUARED_TO_UPDATE_CONTROLLING_ENTITY = 0.5f * 0.5f

// Must be sync to reuse the same vector
private fun ServerClient.asyncHandleMoveEntity(moveEntity: MoveEntity) {
  if (!started) {
    return
  }
  val chunkLoc = worldXYtoChunkCompactLoc(moveEntity.position.x.toInt(), moveEntity.position.y.toInt())
  if (!world.isChunkLoaded(chunkLoc)) {
    logger.warn { "Server sent move entity packet to unloaded chunk" }
    return
  }

  val id = moveEntity.ref.id
  if (id == null) {
    ctx.fatal("Entity ref cannot be parsed")
    return
  }
  val entity = world.getEntity(id)
  if (entity == null) {
    logger.warn { "Cannot move unknown entity '$id'" }
    sendServerBoundPacket(serverBoundEntityRequest(id))
    return
  }
  val serverPos = moveEntity.position
  if (Main.inst().isAuthorizedToChange(entity)) {
    val clientPos = entity.position
    val deltaPos = clientPos.dst2(serverPos.x, serverPos.y)
    if (deltaPos > CLIENT_SERVER_DIFF_SQUARED_TO_UPDATE_CONTROLLING_ENTITY) {
      logger.warn { "Teleporting controlled entity, we're too far away. $deltaPos > $CLIENT_SERVER_DIFF_SQUARED_TO_UPDATE_CONTROLLING_ENTITY" }

      entity.teleport(serverPos)
      entity.setVelocity(moveEntity.velocity)
    } else {
      logger.trace { "We're less than 1/2 blocks away from the server, we don't need to move" }
    }
  } else {
    entity.teleport(serverPos)
    entity.setVelocity(moveEntity.velocity)
    world.postBox2dRunnable {
      val body = entity.box2dBody
      WriteBox2DStateSystem.updatePosition(body, serverPos.x, serverPos.y)
      WriteBox2DStateSystem.updateVelocity(body, moveEntity.velocity.x, moveEntity.velocity.y)
    }
    // Only set look direction if it exists on the entity from before
    moveEntity.lookDirectionOrNull?.let { entity.lookDirectionComponentOrNull?.direction = Direction.valueOf(it) }
  }
}

/**
 * List of despawn reasons that should not be logged as a warning
 */
private val nonWarnDespawnReasons = listOf(DespawnEntity.DespawnReason.UNKNOWN_ENTITY, DespawnEntity.DespawnReason.CHUNK_UNLOADED)

private fun ServerClient.asyncHandleDespawnEntity(despawnEntity: DespawnEntity) {
  val id: String = despawnEntity.ref.id
  val entity: Entity = world.getEntity(id) ?: run {
    if (despawnEntity.despawnReason !in nonWarnDespawnReasons) {
      logger.warn { "Failed to despawn unknown entity with id '$id', reason ${despawnEntity.despawnReason}" }
    }
    return
  }
  logger.debug { "Despawning entity $id with reason ${despawnEntity.despawnReason}" }
  world.removeEntity(entity)
}

private fun ServerClient.asyncHandleBreakingBlock(breakingBlock: Packets.BreakingBlock) {
  for (breakingProgress in breakingBlock.breakingProgressList) {
    breakingBlockCache.put(breakingProgress.blockLocation.toCompact(), breakingProgress.progress)
  }
}

private fun ServerClient.asyncHandleContainerUpdate(containerUpdate: ContainerUpdate) {
  val containerManager = world.worldContainerManager as? ServerClientWorldContainerManager ?: return
  val ownedContainer = containerUpdate.worldContainer.fromProto()
  containerManager.updateContainerFromServer(ownedContainer)
}

private fun ServerClient.asyncHandleHoldingItem(holdingItem: Packets.HoldingItem) {
  val entity = world.getEntity(holdingItem.entityRef.id) ?: return
  val element = holdingItem.element.fromProto()

  val remoteEntityHoldingElementComponent = entity.remoteEntityHoldingElementComponentOrNull
  if (remoteEntityHoldingElementComponent == null) {
    entity.safeWith { RemoteEntityHoldingElement(element) }
  } else {
    remoteEntityHoldingElementComponent.element = element
  }
}
