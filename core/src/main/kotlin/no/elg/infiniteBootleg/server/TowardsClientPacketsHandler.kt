package no.elg.infiniteBootleg.server

import no.elg.infiniteBootleg.events.InitialChunksOfWorldLoadedEvent
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.events.api.EventManager.javaOneShotListener
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity
import no.elg.infiniteBootleg.protobuf.Packets.MoveEntity
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_DESPAWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_INITIAL_CHUNKS_SENT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_LOGIN_STATUS
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_SPAWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_START_GAME
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_UPDATE_CHUNK
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BLOCK_UPDATE
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
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.server.ClientBoundHandler.Companion.TAG
import no.elg.infiniteBootleg.server.SharedInformation.Companion.HEARTBEAT_PERIOD_MS
import no.elg.infiniteBootleg.util.toLocation
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.util.worldXYtoChunkCompactLoc
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.createFallingBlockStandaloneEntity
import no.elg.infiniteBootleg.world.ecs.createMPClientPlayerEntity
import no.elg.infiniteBootleg.world.world.ClientWorld
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Handle packets sent FROM the server THIS client, will quietly drop any packets that are malformed
 *
 * @author Elg
 */
fun ServerClient.handleClientBoundPackets(packet: Packets.Packet) {
  when (packet.type) {
    DX_HEARTBEAT -> if (packet.hasHeartbeat()) {
      handleHeartbeat(packet.heartbeat)
    }

    CB_LOGIN_STATUS -> if (packet.hasServerLoginStatus()) {
      val loginStatus = packet.serverLoginStatus.status
      handleLoginStatus(loginStatus)
    }

    CB_START_GAME -> if (packet.hasStartGame()) {
      handleStartGame(packet.startGame)
    }

    CB_UPDATE_CHUNK -> if (packet.hasUpdateChunk()) {
      handleUpdateChunk(packet.updateChunk)
    }

    CB_DESPAWN_ENTITY -> if (packet.hasDespawnEntity()) {
      handleDespawnEntity(packet.despawnEntity)
    }

    CB_SPAWN_ENTITY -> if (packet.hasSpawnEntity()) {
      if (chunksLoaded) {
        handleSpawnEntity(packet.spawnEntity)
        Main.logger().debug("CB_SPAWN_ENTITY") { "Server sent spawn entity packet\n${packet.spawnEntity.entity}" }
      } else {
        Main.logger().debug("CB_SPAWN_ENTITY") { "Server sent spawn entity packet before chunks loaded packet, ignoring it.\n${packet.spawnEntity.entity}" }
      }
    }

    CB_INITIAL_CHUNKS_SENT -> {
      Main.logger().debug("CB_INITIAL_CHUNKS_SENT", "Setting chunks as loaded")
      chunksLoaded = true
    }

    DX_MOVE_ENTITY -> if (packet.hasMoveEntity() && started) {
      handleMoveEntity(packet.moveEntity)
    }

    DX_BLOCK_UPDATE -> if (packet.hasUpdateBlock() && chunksLoaded) {
      handleBlockUpdate(packet.updateBlock)
    }

    DX_SECRET_EXCHANGE -> if (packet.hasSecretExchange()) {
      handleSecretExchange(packet.secretExchange)
    }

    DX_DISCONNECT -> {
      if (packet.hasDisconnect()) {
        ConnectingScreen.info = "Disconnected: ${packet.disconnect.reason}"
      } else {
        ConnectingScreen.info = "Disconnected: Unknown reason"
      }
      ctx.close()
    }

    DX_WORLD_SETTINGS -> if (packet.hasWorldSettings()) {
      handleWorldSettings(packet.worldSettings)
    }

    UNRECOGNIZED -> ctx.fatal("Unknown packet type received")
    else -> ctx.fatal("Cannot handle packet of type " + packet.type)
  }
}

private fun ServerClient.handleWorldSettings(worldSettings: WorldSettings) {
  val world = this.world
  if (world == null) {
    Main.logger().warn("handleWorldSettings", "Failed to find world")
    return
  }
  Main.logger().debug("handleWorldSettings", "spawn? ${worldSettings.hasSpawn()}, time? ${worldSettings.hasTime()}, time scale? ${worldSettings.hasTimeScale()}")
  if (worldSettings.hasSpawn()) {
    world.spawn = worldSettings.spawn.toLocation()
  }
  if (worldSettings.hasTime()) {
    world.worldTime.time = worldSettings.time
  }
  if (worldSettings.hasTimeScale()) {
    world.worldTime.timeScale = worldSettings.timeScale
  }
}

private fun ServerClient.handleBlockUpdate(blockUpdate: UpdateBlock) {
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
  }
}

private fun ServerClient.handleSpawnEntity(spawnEntity: Packets.SpawnEntity) {
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
  Main.logger().debug("handleSpawnEntity") { "Spawning a ${spawnEntity.entity.type}" }

  when (spawnEntity.entity.type) {
    PLAYER -> {
      val controlled = uuid == spawnEntity.uuid
      val future = world.engine.createMPClientPlayerEntity(world, spawnEntity.entity, controlled)
      if (controlled) {
        futurePlayer = future
      }
    }

    FALLING_BLOCK -> world.engine.createFallingBlockStandaloneEntity(world, spawnEntity.entity)

    else -> Main.logger().error("Cannot spawn a ${spawnEntity.entity.type} yet")
  }
}

private fun ServerClient.handleSecretExchange(secretExchange: SecretExchange) {
  val uuid = secretExchange.entityUUID
  val sharedInformation = SharedInformation(uuid, secretExchange.secret)
  this.sharedInformation = sharedInformation
  Main.logger().debug("LOGIN", "Secret received from sever sending response")
  ctx.writeAndFlush(serverBoundClientSecretResponse(sharedInformation))
}

private fun ServerClient.handleUpdateChunk(updateChunk: UpdateChunk) {
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
  world.updateChunk(chunk)
}

private fun ServerClient.handleStartGame(startGame: StartGame) {
  Main.logger().debug("LOGIN", "Initialization okay, loading world")
  val protoWorld = startGame.world
  this.world = ServerClientWorld(protoWorld, this).apply {
    loadFromProtoWorld(protoWorld)
  }

  if (startGame.controlling.type != PLAYER) {
    ctx.fatal("Can only control a player, got ${startGame.controlling.type}")
  } else {
    this.controllingEntity = startGame.controlling
    Main.logger().debug("LOGIN", "World loaded, player loaded, waiting for chunks")
    ctx.writeAndFlush(serverBoundPacket(SB_CLIENT_WORLD_LOADED))
  }
}

fun ServerClient.handleLoginStatus(loginStatus: ServerLoginStatus.ServerStatus) {
  when (loginStatus) {
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

    ServerLoginStatus.ServerStatus.LOGIN_SUCCESS -> {
      Main.inst().scheduler.executeAsync { handleLoginSuccess() }
    }

    ServerLoginStatus.ServerStatus.UNRECOGNIZED -> {
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

  val futurePlayer = futurePlayer?.orTimeout(10, TimeUnit.SECONDS)
  if (futurePlayer == null) {
    ctx.fatal("Invalid player client side: Did not a receive an entity to control")
    return
  }
  this.futurePlayer = null

  Main.inst().scheduler.executeSync {
    dispatchEvent(InitialChunksOfWorldLoadedEvent(world))
  }

  val worldFuture: CompletableFuture<ClientWorld> = CompletableFuture<ClientWorld>().orTimeout(10, TimeUnit.SECONDS)
  javaOneShotListener(WorldLoadedEvent::class.java) {
    Main.logger().debug("ClientWorld") { "Completing World loaded future" }
    worldFuture.complete(it.world as ClientWorld)
  }

  ConnectingScreen.info = "Waiting for world to be ready..."

  val handledPlayer = futurePlayer.whenCompleteAsync { player, e ->
    if (e != null) {
      ctx.fatal("Invalid player client side\n  ${e::class.simpleName}: ${e.message}")
      return@whenCompleteAsync
    } else {
      this@handleLoginSuccess.player = player
      player.box2d.enableGravity()
      Main.logger().debug("handleSpawnEntity", "Server sent the entity to control")
    }
  }

  worldFuture.thenCombine(handledPlayer) { _, _ ->
    Main.inst().scheduler.executeSync {
      started = true
      ClientMain.inst().screen = WorldScreen(world, false)
      setupHeartbeat()
      Main.logger().debug("LOGIN", "Logged into server successfully")
    }
  }
}

private fun ServerClient.setupHeartbeat() {
  val sharedInformation = sharedInformation!!
  sharedInformation.heartbeatTask = ctx.executor().scheduleAtFixedRate({
//          Main.logger().log("Sending heartbeat to server")
    ctx.writeAndFlush(serverBoundHeartbeat())
    if (sharedInformation.lostConnection()) {
      ctx.fatal("Server stopped responding, heartbeats not received")
    }
  }, HEARTBEAT_PERIOD_MS, HEARTBEAT_PERIOD_MS, TimeUnit.MILLISECONDS)
}

private fun ServerClient.handleMoveEntity(moveEntity: MoveEntity) {
  val world = world
  if (world == null) {
    Main.logger().warn("handleMoveEntity", "Failed to find world")
    return
  }
  val chunkLoc = worldXYtoChunkCompactLoc(moveEntity.position.x.toInt(), moveEntity.position.y.toInt())
  if (!world.isChunkLoaded(chunkLoc)) {
    // Chunk is not loaded, so ignore this entity update
    // TODO (from the server) only send entity packets of loaded chunks
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
    ctx.writeAndFlush(serverBoundEntityRequest(uuid))
    return
  }
  entity.teleport(moveEntity.position.x, moveEntity.position.y)
  entity.setVelocity(moveEntity.velocity.x, moveEntity.velocity.y)
}

private fun ServerClient.handleDespawnEntity(despawnEntity: DespawnEntity) {
  val world = world
  if (world == null) {
    Main.logger().error("handleDespawnEntity", "Failed to find world")
    return
  }
  val uuid = despawnEntity.uuid
  if (uuid == null) {
    Main.logger().error("handleDespawnEntity", "Failed to parse UUID '${despawnEntity.uuid}'")
    return
  }
  val entity = world.getEntity(uuid)
  if (entity == null) {
    Main.logger().error("handleDespawnEntity", "Failed to get entity with uuid '${despawnEntity.uuid}'")
    return
  }
  world.removeEntity(entity)
}

private fun ServerClient.handleHeartbeat(heartbeat: Packets.Heartbeat) {
//  Main.logger().debug("Heartbeat", "Client got server heartbeat: " + heartbeat.keepAliveId)
  sharedInformation?.beat() ?: Main.logger().error("handleHeartbeat", "Failed to beat, because of null shared information")
}
