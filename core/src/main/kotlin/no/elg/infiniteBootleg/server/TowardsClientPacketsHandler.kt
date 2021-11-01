package no.elg.infiniteBootleg.server

import java.util.UUID
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.MoveEntity
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_LOGIN_STATUS
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_SPAWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_START_GAME
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_UPDATE_CHUNK
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BLOCK_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_DISCONNECT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_HEARTBEAT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_MOVE_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_SECRET_EXCHANGE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CLIENT_WORLD_LOADED
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.UNRECOGNIZED
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.protobuf.Packets.StartGame
import no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock
import no.elg.infiniteBootleg.protobuf.Packets.UpdateChunk
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType.PLAYER
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.util.fromUUIDOrNull
import no.elg.infiniteBootleg.util.toLocation
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.subgrid.Entity
import no.elg.infiniteBootleg.world.subgrid.enitites.Player

/**
 * @author Elg
 */
fun ServerClient.handleClientBoundPackets(packet: Packets.Packet) {
  when (packet.type) {
    CB_LOGIN_STATUS -> {
      if (packet.hasServerLoginStatus()) {
        val loginStatus = packet.serverLoginStatus.status
        loginStatus(loginStatus)
      }
    }
    CB_START_GAME -> {
      if (packet.hasStartGame()) {
        startGame(packet.startGame)
      }
    }
    CB_UPDATE_CHUNK -> {
      if (packet.hasUpdateChunk()) {
        updateChunk(packet.updateChunk)
      }
    }

    CB_SPAWN_ENTITY -> {
      if (packet.hasSpawnEntity()) {
        handleSpawnEntity(packet.spawnEntity)
      }
    }

    DX_HEARTBEAT -> {
      if (packet.hasHeartbeat()) {
        ctx.writeAndFlush(packet)
      }
    }
    DX_MOVE_ENTITY -> {
      if (packet.hasMoveEntity()) {
        handleMoveEntity(packet.moveEntity)
      }
    }
    DX_BLOCK_UPDATE -> {
      if (packet.hasUpdateBlock()) {
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

    UNRECOGNIZED -> {
      ctx.fatal("Unknown packet type received")
    }
    else -> {
      ctx.fatal("Cannot handle packet of type " + packet.type)
    }
  }
}

fun ServerClient.handleBlockUpdate(blockUpdate: UpdateBlock) {
  Main.inst().scheduler.executeSync {
    val world = this.world ?: return@executeSync
    val worldX = blockUpdate.pos.x
    val worldY = blockUpdate.pos.y
    val protoBlock = if (blockUpdate.hasBlock()) blockUpdate.block else null
    world.setBlock(worldX, worldY, protoBlock, false)
  }
}

fun ServerClient.handleSpawnEntity(spawnEntity: Packets.SpawnEntity) {
  Main.inst().scheduler.executeSync {
    val world = this.world ?: return@executeSync
    val pos = spawnEntity.entity.position.toLocation()
    val chunk = world.getChunk(pos) ?: return@executeSync
    Entity.load(world, chunk, spawnEntity.entity)
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

fun ServerClient.updateChunk(updateChunk: UpdateChunk) {
  Main.inst().scheduler.executeSync {
    val world = this.world ?: return@executeSync
    val chunk = world.chunkLoader.clientLoad(updateChunk.chunk) ?: return@executeSync
    world.updateChunk(chunk)
  }
}

fun ServerClient.startGame(startGame: StartGame) {
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

fun ServerClient.loginStatus(loginStatus: ServerLoginStatus.ServerStatus) {
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
        ClientMain.inst().screen = WorldScreen(world, false)
        world.removePlayer(UUID.fromString(entity.uuid))
        val player = Player(world, entity)
        if (player.isInvalid) {
          ctx.fatal("Invalid player client side")
        } else {
          player.name = name
          world.addEntity(player)
          world.worldTicker.start()
          player.giveControls()
        }
      }
    }
    ServerLoginStatus.ServerStatus.UNRECOGNIZED -> {
      ctx.fatal("Unrecognized server status")
      return
    }
  }
}

fun ServerClient.handleMoveEntity(moveEntity: MoveEntity) {
  val uuid = fromUUIDOrNull(moveEntity.uuid)
  if (uuid == null) {
    ctx.fatal("UUID cannot be parsed '${moveEntity.uuid}'")
    return
  }
  if (uuid == this.credentials?.entityUUID) {
    //Don't move ourselves
    return
  }
  val world = world
  if (world == null) {
    Main.logger().warn("Failed to find world")
    //will be spammed when logging in
    return
  }
  val entity = world.getEntity(uuid)
  if (entity == null) {
//    ctx.fatal("Cannot move unknown entity '${moveEntity.uuid}'")
    Main.logger().warn("Cannot move unknown entity '${moveEntity.uuid}'")
    return
  }
  entity.translate(moveEntity.position.x, moveEntity.position.y, moveEntity.velocity.x, moveEntity.velocity.y, false)
}
