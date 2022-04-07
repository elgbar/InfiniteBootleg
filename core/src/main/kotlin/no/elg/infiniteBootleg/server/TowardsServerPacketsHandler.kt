package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.ServerMain
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.ChunkRequest
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.UNKNOWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.EntityRequest
import no.elg.infiniteBootleg.protobuf.Packets.Heartbeat
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
import no.elg.infiniteBootleg.server.SharedInformation.Companion.HEARTBEAT_PERIOD_MS
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.util.fromUUIDOrNull
import no.elg.infiniteBootleg.util.toLocation
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.loader.WorldLoader
import no.elg.infiniteBootleg.world.render.ChunksInView
import no.elg.infiniteBootleg.world.subgrid.enitites.Player
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit

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
    DX_HEARTBEAT -> {
      if (packet.hasHeartbeat()) {
        handleHeartbeat(ctx, packet.heartbeat)
      }
    }
    DX_MOVE_ENTITY -> {
      if (packet.hasMoveEntity()) {
        handlePlayerUpdate(ctx, packet.moveEntity)
      }
    }
    DX_BLOCK_UPDATE -> {
      if (packet.hasUpdateBlock()) {
        handleBlockUpdate(ctx, packet.updateBlock)
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
  val shared = ctx.getSharedInformation()
  if (shared == null) {
    ctx.fatal("No secret with this channel, send login request first")
    return
  }

  try {
    val uuid = UUID.fromString(secretExchange.entityUUID)
    if (uuid != shared.entityUUID || secretExchange.secret != shared.secret) {
      ctx.fatal("Wrong shared information returned by client")
      return
    }
    val player = Main.inst().world?.getPlayer(shared.entityUUID)
    if (player != null) {
      ctx.writeAndFlush(clientBoundStartGamePacket(player))
    } else {
      ctx.fatal("handleSecretExchange: Failed to find entity with the given uuid")
    }
  } catch (e: Exception) {
    ctx.fatal("handleSecretExchange: Failed to parse entity")
    e.printStackTrace()
  }
}

private fun handleBlockUpdate(ctx: ChannelHandlerContext, blockUpdate: UpdateBlock) {
  val worldX = blockUpdate.pos.x
  val worldY = blockUpdate.pos.y
  if (isLocInView(ctx, worldX, worldY)) {
    val protoBlock = if (blockUpdate.hasBlock()) blockUpdate.block else null
    ServerMain.inst().serverWorld.setBlock(worldX, worldY, protoBlock, true)
  }
}

private fun handleChunkRequest(ctx: ChannelHandlerContext, chunkRequest: ChunkRequest) {
  val chunkLoc = chunkRequest.chunkLocation
  val serverWorld = ServerMain.inst().serverWorld

  // Only send chunks which the player is allowed to see
  if (isChunkInView(ctx, chunkLoc.x, chunkLoc.y)) {
    val chunk = serverWorld.getChunk(chunkLoc.x, chunkLoc.y) ?: return // if no chunk, don't send a chunk update
    ctx.writeAndFlush(clientBoundUpdateChunkPacket(chunk))
  }
}

private fun handleClientsWorldLoaded(ctx: ChannelHandlerContext) {
  val world = ServerMain.inst().serverWorld
  val shared = ctx.getSharedInformation()
  val player = ctx.getCurrentPlayer()
  if (player == null || shared == null) {
    ctx.fatal("Player not loaded server-side")
    return
  }

  Main.logger().debug("LOGIN", "Client world ready, sending chunks to client ${player.name}")

  // Send chunk packets to client
  val ix = CoordUtil.worldToChunk(player.blockX)
  val iy = CoordUtil.worldToChunk(player.blockY)
  for (cx in -Settings.viewDistance..Settings.viewDistance) {
    for (cy in -Settings.viewDistance..Settings.viewDistance) {
      val chunk = world.getChunk(ix + cx, iy + cy) ?: continue
      ctx.write(clientBoundUpdateChunkPacket(chunk))
    }
    ctx.flush()
    Main.logger().debug("LOGIN") {
      val sent = (cx + Settings.viewDistance + 1) * (Settings.viewDistance * 2 + 1)
      val total = (Settings.viewDistance + Settings.viewDistance + 1) * (Settings.viewDistance + Settings.viewDistance + 1)
      "Sent $sent/$total chunks sent to player ${player.name}"
    }
  }
  ctx.writeAndFlush(clientBoundPacket(CB_INITIAL_CHUNKS_SENT))
  Main.logger().debug("LOGIN", "Initial chunks sent to player ${player.name}")

  for (entity in world.entities) {
    Main.logger().log("Sending entity ${entity.simpleName()} (${entity.uuid}) to client")
    ctx.write(clientBoundSpawnEntity(entity))
  }
  ctx.flush()
  Main.logger().debug("LOGIN", "Initial entities sent to player ${player.name}")

  ctx.writeAndFlush(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.LOGIN_SUCCESS))

  shared.heartbeatTask = ctx.executor().scheduleAtFixedRate({
//    Main.logger().log("Sending heartbeat to client")
    ctx.writeAndFlush(clientBoundHeartbeat())
    if (shared.lostConnection()) {
      Main.logger().error("Heartbeat", "Client stopped responding, heartbeats not received")
      ctx.close()
    }
  }, HEARTBEAT_PERIOD_MS, HEARTBEAT_PERIOD_MS, TimeUnit.MILLISECONDS)
  Main.logger().log("Player " + player.name + " joined")
}

private fun handleLoginPacket(ctx: ChannelHandlerContext, login: Packets.Login) {
  val version = Util.getVersion()
  if (login.version != version) {
    ctx.fatal("Version mismatch! Client: '${login.version}' Server: '$version'")
    return
  }
  Main.logger().debug("LOGIN", "Login request received by " + login.username + " uuid " + login.uuid)

  val world = ServerMain.inst().serverWorld
  val uuid = fromUUIDOrNull(login.uuid)
  if (uuid == null) {
    Main.logger().error("handleLoginPacket", "Failed to parse UUID '${login.uuid}'")
    return
  }

  if (world.hasPlayer(uuid)) {
    ctx.writeAndFlush(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.ALREADY_LOGGED_IN))
    ctx.close()
    return
  }
  // Client is good to login
  ctx.writeAndFlush(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.PROCEED_LOGIN))
  val player = WorldLoader.getServerPlayer(world, uuid)
  if (player.isInvalid) {
    ctx.fatal("Failed to spawn player server side, player is invalid")
    return
  }
  require(player.uuid == uuid)
  // username might have changed
  if (player.name != login.username) {
    Main.logger().debug("LOGIN", "Player name has changed from '" + player.name + "' to '" + login.username + "'")
    player.name = login.username
    WorldLoader.saveServerPlayer(player)
  }

  val secret = UUID.nameUUIDFromBytes(ByteArray(128).also { secureRandom.nextBytes(it) })
  val sharedInformation = SharedInformation(player.uuid, secret.toString())
  ServerBoundHandler.clients[ctx.channel()] = sharedInformation

  Main.logger().debug("LOGIN", "Secret sent to player, waiting for confirmation")
  // Exchange the UUID and secret, which will be used to verify the sender, kinda like a bearer bond.
  ctx.writeAndFlush(clientBoundSecretExchange(sharedInformation))
}

private fun handleEntityRequest(ctx: ChannelHandlerContext, entityRequest: EntityRequest) {
  val world = ServerMain.inst().serverWorld

  val uuid = fromUUIDOrNull(entityRequest.uuid)
  if (uuid == null) {
    Main.logger().error("handleEntityRequest", "Failed to parse UUID '${entityRequest.uuid}'")
    return
  }
  val entity = world.getEntity(uuid)
  if (entity != null && isLocInView(ctx, entity.position.x.toInt(), entity.position.y.toInt())) {
    ctx.writeAndFlush(clientBoundSpawnEntity(entity))
  } else {
    ctx.writeAndFlush(clientBoundDespawnEntity(uuid, UNKNOWN_ENTITY))
  }
}

private fun handleHeartbeat(ctx: ChannelHandlerContext, heartbeat: Heartbeat) {
//  Main.logger().debug("Heartbeat","Server got client (" + ctx.getCurrentPlayer()?.name + ") heartbeat: " + heartbeat.keepAliveId)
  ctx.getSharedInformation()?.beat()
}

// ///////////
//  UTILS  //
// ///////////

private fun ChannelHandlerContext.getSharedInformation(): SharedInformation? {
  return ServerBoundHandler.clients[this.channel()]
}

private fun ChannelHandlerContext.getCurrentPlayer(): Player? {
  val uuid = getSharedInformation()?.entityUUID ?: return null
  return ServerMain.inst().serverWorld.getPlayer(uuid)
}

/**
 * Use to check if something should be sent to a client
 */
private fun chunksInView(ctx: ChannelHandlerContext): ChunksInView? {
  val serverWorld = ServerMain.inst().serverWorld
  val uuid = ctx.getSharedInformation()?.entityUUID
  if (uuid == null) {
    Main.logger().error("handleChunkRequest", "Failed to get UUID of requesting entity")
    return null
  }
  val chunksInView = serverWorld.render.getClient(uuid)
  if (chunksInView == null) {
    Main.logger().error("handleChunkRequest", "Failed to get chunks in view")
    return null
  }
  return chunksInView
}

private fun isChunkInView(ctx: ChannelHandlerContext, chunkX: Int, chunkY: Int): Boolean {
  return chunksInView(ctx)?.isInView(chunkX, chunkY) ?: false
}

private fun isLocInView(ctx: ChannelHandlerContext, worldX: Int, worldY: Int): Boolean {
  return chunksInView(ctx)?.isInView(CoordUtil.worldToChunk(worldX), CoordUtil.worldToChunk(worldY)) ?: false
}
