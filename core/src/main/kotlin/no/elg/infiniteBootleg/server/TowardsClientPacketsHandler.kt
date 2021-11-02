package no.elg.infiniteBootleg.server

import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
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
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType.PLAYER
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.server.ClientBoundHandler.TAG
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.util.fromUUIDOrNull
import no.elg.infiniteBootleg.util.toLocation
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.subgrid.Entity
import no.elg.infiniteBootleg.world.subgrid.LivingEntity
import java.util.UUID

/**
 * @author Elg
 */
fun ServerClient.handleClientBoundPackets(packet: Packets.Packet) {
  when (packet.type) {
    CB_LOGIN_STATUS -> {
      if (packet.hasServerLoginStatus()) {
        val loginStatus = packet.serverLoginStatus.status
        handleLoginStatus(loginStatus)
      }
    }
    CB_START_GAME -> {
      if (packet.hasStartGame()) {
        handleStartGame(packet.startGame)
      }
    }
    CB_UPDATE_CHUNK -> {
      if (packet.hasUpdateChunk()) {
        handleUpdateChunk(packet.updateChunk)
      }
    }
    CB_DESPAWN_ENTITY -> {
      if (packet.hasDespawnEntity()) {
        handleDespawnEntity(packet.despawnEntity)
      }
    }

    CB_SPAWN_ENTITY -> {
      if (packet.hasSpawnEntity() && chunksLoaded) {
        handleSpawnEntity(packet.spawnEntity)
      }
    }
    CB_INITIAL_CHUNKS_SENT -> chunksLoaded = true
    DX_HEARTBEAT -> {
      if (packet.hasHeartbeat()) {
        ctx.writeAndFlush(packet)
      }
    }
    DX_MOVE_ENTITY -> {
      if (packet.hasMoveEntity() && started) {
        handleMoveEntity(packet.moveEntity)
      }
    }
    DX_BLOCK_UPDATE -> {
      if (packet.hasUpdateBlock() && chunksLoaded) {
        handleBlockUpdate(packet.updateBlock)
      }
    }
    DX_SECRET_EXCHANGE -> {
      if (packet.hasSecretExchange()) {
        handleSecretExchange(packet.secretExchange)
      }
    }
    DX_DISCONNECT -> {
      if (packet.hasDisconnect()) {
        ConnectingScreen.info = "Disconnected: ${packet.disconnect.reason}"
      } else {
        ConnectingScreen.info = "Disconnected: Unknown reason"
      }
      ctx.close()
    }
    DX_WORLD_SETTINGS -> {
      if (packet.hasWorldSettings()) {
        handleWorldSettings(packet.worldSettings)
      }
    }

    UNRECOGNIZED -> {
      ctx.fatal("Unknown packet type received")
    }
    else -> {
      ctx.fatal("Cannot handle packet of type " + packet.type)
    }
  }
}

private fun ServerClient.handleWorldSettings(worldSettings: WorldSettings) {
  val world = this.world
  if (world == null) {
    Main.logger().warn("handleWorldSettings", "Failed to find world")
    return
  }
  Main.logger().log("handleWorldSettings: spawn? ${worldSettings.hasSpawn()}, time? ${worldSettings.hasTime()}, time scale? ${worldSettings.hasTimeScale()}")
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
  Main.inst().scheduler.executeSync {
    val world = this.world
    if (world == null) {
      Main.logger().warn("handleBlockUpdate", "Failed to find world")
      return@executeSync
    }
    val worldX = blockUpdate.pos.x
    val worldY = blockUpdate.pos.y
    val protoBlock = if (blockUpdate.hasBlock()) blockUpdate.block else null
    world.setBlock(worldX, worldY, protoBlock, false)
  }
}

private fun ServerClient.handleSpawnEntity(spawnEntity: Packets.SpawnEntity) {
  Main.inst().scheduler.executeSync {
    val world = this.world
    if (world == null) {
      Main.logger().warn("handleSpawnEntity", "Failed to find world")
      return@executeSync
    }
    val chunkPos = CoordUtil.worldToChunk(spawnEntity.entity.position.toLocation())
    val chunk = world.getChunk(chunkPos)
    if (chunk == null) {
      Main.logger().warn("handleSpawnEntity", "Chunk not loaded $chunkPos")
      return@executeSync
    }
    Main.logger().log("Will spawn entity ${spawnEntity.entity.uuid} " + spawnEntity.entity.type)
    val loaded = Entity.load(world, chunk, spawnEntity.entity)
    if (loaded is LivingEntity && uuid == loaded.uuid) {
      // it's us!
      loaded.enableGravity()
    }
  }
}

private fun ServerClient.handleSecretExchange(secretExchange: SecretExchange) {
  val uuid = try {
    UUID.fromString(secretExchange.entityUUID)
  } catch (e: IllegalArgumentException) {
    ctx.fatal("Failed to decode entity UUID ${secretExchange.entityUUID}")
    return
  }
  val connectionCredentials = ConnectionCredentials(uuid, secretExchange.secret)
  credentials = connectionCredentials
  ctx.writeAndFlush(serverBoundClientSecretResponse(connectionCredentials))
}

private fun ServerClient.handleUpdateChunk(updateChunk: UpdateChunk) {
  val entities = updateChunk.chunk.entitiesCount
  if (entities > 0) {
    Main.logger().warn(TAG, "Got $entities entities in chunk update")
  }

  fun tryUpdateChunk() {
    val world = this.world
    if (world == null) {
      Main.logger().warn(TAG, "Failed to find the world to update chunk ${updateChunk.chunk.position}")
      Main.inst().scheduler.executeSync { tryUpdateChunk() }
      return
    }
    val chunk = world.chunkLoader.clientLoad(updateChunk.chunk)
    if (chunk == null) {
      Main.logger().warn(TAG, "Failed to load the chunk from proto")
      return
    }
    world.updateChunk(chunk)
  }

  val exec = Runnable {
    tryUpdateChunk()
  }
  Main.inst().scheduler.executeSync(exec)
}

private fun ServerClient.handleStartGame(startGame: StartGame) {
  val protoWorld = startGame.world
  Main.inst().scheduler.executeSync {
    this.world = World(protoWorld).apply {
      serverLoad(protoWorld)
    }
    if (startGame.controlling.type != PLAYER) {
      ctx.fatal("Can only control a player, got ${startGame.controlling.type}")
    } else {
      this.controllingEntity = startGame.controlling
      ctx.writeAndFlush(serverBoundPacket(SB_CLIENT_WORLD_LOADED))
    }
  }
}

private fun ServerClient.handleLoginStatus(loginStatus: ServerLoginStatus.ServerStatus) {
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
      ConnectingScreen.info = "Logging in..."
    }
    ServerLoginStatus.ServerStatus.LOGIN_SUCCESS -> {
      val world = world
      val entity = controllingEntity
      if (world == null || entity == null) {
        ctx.fatal("Failed to get world and/or entity")
        return
      }
      ConnectingScreen.info = "Login success!"
      Main.inst().scheduler.executeSync {

        val player = world.getPlayer(UUID.fromString(entity.uuid))
        if (player == null || player.isInvalid) {
          ctx.fatal("Invalid player client side reason: ${if (player == null) "Player is null" else "Player invalid"}")
        } else {
          world.worldTicker.start()
          ClientMain.inst().screen = WorldScreen(world, false)
          player.giveControls()
          started = true
        }
      }
    }
    ServerLoginStatus.ServerStatus.UNRECOGNIZED -> {
      ctx.fatal("Unrecognized server status")
      return
    }
  }
}

private fun ServerClient.handleMoveEntity(moveEntity: MoveEntity) {
  val uuid = fromUUIDOrNull(moveEntity.uuid)
  if (uuid == null) {
    ctx.fatal("UUID cannot be parsed '${moveEntity.uuid}'")
    return
  }
  val world = world
  if (world == null) {
    Main.logger().warn("handleMoveEntity", "Failed to find world")
    return
  }
  val entity = world.getEntity(uuid)
  if (entity == null) {
    Main.logger().warn("Cannot move unknown entity '${moveEntity.uuid}'")
    ctx.writeAndFlush(serverBoundEntityRequest(uuid))
    return
  }
  entity.translate(moveEntity.position.x, moveEntity.position.y, moveEntity.velocity.x, moveEntity.velocity.y, moveEntity.lookAngleDeg, false)
}

private fun ServerClient.handleDespawnEntity(despawnEntity: DespawnEntity) {
  val world = world
  if (world == null) {
    Main.logger().warn("handleDespawnEntity", "Failed to find world")
    return
  }
  val uuid = fromUUIDOrNull(despawnEntity.uuid) ?: return
  val entity = world.getEntity(uuid) ?: return
  world.removeEntity(entity)
}
