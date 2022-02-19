package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.ServerMain
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.ChunkRequest
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.UNKNOWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.EntityRequest
import no.elg.infiniteBootleg.protobuf.Packets.MoveEntity
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_INITIAL_CHUNKS_SENT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BLOCK_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_DISCONNECT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_HEARTBEAT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_MOVE_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_SECRET_EXCHANGE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_WORLD_SETTINGS
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CHUNK_REQUEST
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CLIENT_WORLD_LOADED
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_ENTITY_REQUEST
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_LOGIN
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.UNRECOGNIZED
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock
import no.elg.infiniteBootleg.protobuf.Packets.WorldSettings
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.util.fromUUIDOrNull
import no.elg.infiniteBootleg.util.toLocation
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.loader.WorldLoader
import no.elg.infiniteBootleg.world.subgrid.enitites.Player
import java.security.SecureRandom
import java.util.UUID

private val secureRandom = SecureRandom.getInstanceStrong()

/**
 * Handle packets sent to the server, will quietly drop any packets that are malformed
 * @author Elg
 */
fun handleServerBoundPackets(ctx: ChannelHandlerContext, packet: Packets.Packet) {
  when (packet.type) {
    SB_LOGIN -> {
      if (packet.hasLogin()) {
        handleLoginPacket(ctx, packet.login)
      }
    }
    SB_CLIENT_WORLD_LOADED -> handleClientsWorldLoaded(ctx)
    SB_CHUNK_REQUEST -> {
      if (packet.hasChunkRequest()) {
        handleChunkRequest(ctx, packet.chunkRequest)
      }
    }
    DX_SECRET_EXCHANGE -> {
      if (packet.hasSecretExchange()) {
        handleSecretExchange(ctx, packet.secretExchange)
      }
    }
    DX_HEARTBEAT -> TODO()
    DX_MOVE_ENTITY -> {
      if (packet.hasMoveEntity()) {
        handlePlayerUpdate(ctx, packet.moveEntity)
      }
    }
    DX_BLOCK_UPDATE -> {
      if (packet.hasUpdateBlock()) {
        handleBlockUpdate(packet.updateBlock)
      }
    }
    DX_DISCONNECT -> {
      Main.logger().log("Client sent disconnect packet. Reason: " + if (packet.hasDisconnect()) packet.disconnect?.reason else "No reason given")
      ctx.close()
    }
    SB_ENTITY_REQUEST -> {
      if (packet.hasEntityRequest()) {
        handleEntityRequest(ctx, packet.entityRequest)
      }
    }
    DX_WORLD_SETTINGS -> {
      if (packet.hasWorldSettings()) {
        handleWorldSettings(ctx, packet.worldSettings)
      }
    }

    UNRECOGNIZED -> ctx.fatal("Unknown packet type received")
    else -> ctx.fatal("Cannot handle packet of type " + packet.type)
  }
}

private fun handleWorldSettings(ctx: ChannelHandlerContext, worldSettings: WorldSettings) {
  Main.logger().log("handleWorldSettings: spawn? ${worldSettings.hasSpawn()}, time? ${worldSettings.hasTime()}, time scale? ${worldSettings.hasTimeScale()}")
  val world = ServerMain.inst().serverWorld
  var spawn: Location? = null
  var time: Float? = null
  var timeScale: Float? = null

  if (worldSettings.hasSpawn()) {
    world.spawn = worldSettings.spawn.toLocation()
    spawn = world.spawn
  }
  if (worldSettings.hasTime()) {
    world.worldTime.time = worldSettings.time
    time = worldSettings.time
  }
  if (worldSettings.hasTimeScale()) {
    world.worldTime.timeScale = worldSettings.timeScale
    timeScale = worldSettings.timeScale
  }
  // Rebroadcast the packet to all clients to stay in sync
  broadcast(clientBoundWorldSettings(spawn, time, timeScale)) { c, _ -> c != ctx.channel() }
}

private fun handlePlayerUpdate(ctx: ChannelHandlerContext, moveEntity: MoveEntity) {
  val player = ctx.getCurrentPlayer()
  if (player == null) {
    ctx.fatal("No server side player found!")
    return
  }
  if (player.uuid.toString() != moveEntity.uuid) {
    ctx.fatal("Client tried to update someone else")
    return
  }
  player.translate(moveEntity.position.x, moveEntity.position.y, moveEntity.velocity.x, moveEntity.velocity.y, moveEntity.lookAngleDeg, false)
}

private fun handleSecretExchange(ctx: ChannelHandlerContext, secretExchange: SecretExchange) {
  val cc = ServerBoundHandler.clients[ctx.channel()]
  if (cc == null) {
    ctx.fatal("No secret with this channel, send login request first")
    return
  }

  try {
    val uuid = UUID.fromString(secretExchange.entityUUID)
    if (uuid != cc.entityUUID || secretExchange.secret != cc.secret) {
      ctx.fatal("Wrong secret returned!")
      return
    }
    val world = ServerMain.inst().serverWorld
    val player = world.getPlayer(uuid)
    if (player != null) {
      ctx.writeAndFlush(clientBoundStartGamePacket(player))
    } else {
      ctx.fatal("handleSecretExchange: Failed secret response")
    }
  } catch (e: Exception) {
    ctx.fatal("handleSecretExchange: Failed to parse entity")
    e.printStackTrace()
    return
  }
}

private fun handleBlockUpdate(blockUpdate: UpdateBlock) {
  val worldX = blockUpdate.pos.x
  val worldY = blockUpdate.pos.y
  val protoBlock = if (blockUpdate.hasBlock()) blockUpdate.block else null
  ServerMain.inst().serverWorld.setBlock(worldX, worldY, protoBlock, true)
}

private fun handleChunkRequest(ctx: ChannelHandlerContext, chunkRequest: ChunkRequest) {
  val chunkLoc = Location.fromVector2i(chunkRequest.chunkLocation)
  val chunk = ServerMain.inst().serverWorld.getChunk(chunkLoc) ?: return // if no chunk, don't send a chunk update
  ctx.writeAndFlush(clientBoundUpdateChunkPacket(chunk))
}

private fun handleClientsWorldLoaded(ctx: ChannelHandlerContext) {
  val world = ServerMain.inst().serverWorld
  val player = ctx.getCurrentPlayer()
  if (player == null) {
    ctx.fatal("Player not loaded server-side")
    return
  }

  // Send chunk packets to client
  val ix = CoordUtil.worldToChunk(player.blockX)
  val iy = CoordUtil.worldToChunk(player.blockY)
  for (cx in -Settings.chunkRadius..Settings.chunkRadius) {
    for (cy in -Settings.chunkRadius..Settings.chunkRadius) {
      val chunk = world.getChunk(ix + cx, iy + cy) ?: continue
      ctx.write(clientBoundUpdateChunkPacket(chunk))
    }
    ctx.flush()
  }
  ctx.writeAndFlush(clientBoundPacket(CB_INITIAL_CHUNKS_SENT))

  for (entity in world.entities) {
    Main.logger().log("Sending ${entity.simpleName()} ${entity.hudDebug()} to client")
    ctx.write(clientBoundSpawnEntity(entity))
  }
  ctx.flush()

  ctx.writeAndFlush(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.LOGIN_SUCCESS))
  Main.logger().log("Player " + player.name + " joined")
  broadcast(clientBoundSpawnEntity(player)) { c, _ -> c != ctx.channel() }
}

private fun handleLoginPacket(ctx: ChannelHandlerContext, login: Packets.Login) {
  val version = Util.getVersion()
  if (login.version != version) {
    ctx.fatal("Version mismatch! Client: '${login.version}' Server: '$version'")
    return
  }

  val world = ServerMain.inst().serverWorld
  val uuid = fromUUIDOrNull(login.uuid) ?: return

  if (world.hasPlayer(uuid)) {
    ctx.writeAndFlush(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.ALREADY_LOGGED_IN))
    ctx.close()
    return
  }
  // Client is good to login
  ctx.writeAndFlush(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.PROCEED_LOGIN))
  val player = WorldLoader.getServerPlayer(world, uuid)
  if (player.isInvalid) {
    ctx.fatal("Failed to spawn player server side")
    return
  }
  require(player.uuid == uuid)
  player.name = login.username
  WorldLoader.saveServerPlayer(player)

  val secret = UUID.nameUUIDFromBytes(ByteArray(128).also { secureRandom.nextBytes(it) })

  val connectionCredentials = ConnectionCredentials(player.uuid, secret.toString())
  ServerBoundHandler.clients[ctx.channel()] = connectionCredentials

  // Exchange the UUID and secret, which will be used to verify the sender, kinda like a bearer bond.
  ctx.writeAndFlush(clientBoundSecretExchange(connectionCredentials))
}

private fun handleEntityRequest(ctx: ChannelHandlerContext, entityRequest: EntityRequest) {
  val world = ServerMain.inst().serverWorld

  val uuid = fromUUIDOrNull(entityRequest.uuid) ?: return
  val entity = world.getEntity(uuid)
  if (entity != null) {
    ctx.writeAndFlush(clientBoundSpawnEntity(entity))
  } else {
    ctx.writeAndFlush(clientBoundDespawnEntity(uuid, UNKNOWN_ENTITY))
//    Main.logger().warn("handleEntityRequest", "Unknown entity requested UUID: ${entityRequest.uuid}")
  }
}

private fun ChannelHandlerContext.getClientCredentials(): ConnectionCredentials? {
  return ServerBoundHandler.clients[this.channel()]
}

private fun ChannelHandlerContext.getCurrentPlayer(): Player? {
  val uuid = getClientCredentials()?.entityUUID ?: return null
  return ServerMain.inst().serverWorld.getPlayer(uuid)
}
