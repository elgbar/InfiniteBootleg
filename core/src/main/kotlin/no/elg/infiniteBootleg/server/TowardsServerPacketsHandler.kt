package no.elg.infiniteBootleg.server

import com.badlogic.ashley.core.Entity
import io.netty.channel.ChannelHandlerContext
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.console.logPacket
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.main.ServerMain
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.ChunkRequest
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.UNKNOWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Disconnect
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
import no.elg.infiniteBootleg.protobuf.blockOrNull
import no.elg.infiniteBootleg.protobuf.chunkRequestOrNull
import no.elg.infiniteBootleg.protobuf.disconnectOrNull
import no.elg.infiniteBootleg.protobuf.entityRequestOrNull
import no.elg.infiniteBootleg.protobuf.loginOrNull
import no.elg.infiniteBootleg.protobuf.lookDirectionOrNull
import no.elg.infiniteBootleg.protobuf.moveEntityOrNull
import no.elg.infiniteBootleg.protobuf.secretExchangeOrNull
import no.elg.infiniteBootleg.protobuf.updateBlockOrNull
import no.elg.infiniteBootleg.protobuf.worldSettingsOrNull
import no.elg.infiniteBootleg.server.SharedInformation.Companion.HEARTBEAT_PERIOD_MS
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.toCompact
import no.elg.infiniteBootleg.util.toComponentsString
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.name
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameComponent
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.shouldSendToClients
import no.elg.infiniteBootleg.world.loader.WorldLoader
import no.elg.infiniteBootleg.world.render.ChunksInView
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private val secureRandom = SecureRandom.getInstanceStrong()

val scheduler by lazy { Main.inst().scheduler }

/**
 * Handle packets sent TO the server FROM a client, will quietly drop any packets that are malformed
 * @author Elg
 */
fun handleServerBoundPackets(ctx: ChannelHandlerContextWrapper, packet: Packets.Packet) {
  logPacket("server<-client", packet)
  when (packet.type) {
    DX_HEARTBEAT -> handleHeartbeat(ctx)
    DX_MOVE_ENTITY -> packet.moveEntityOrNull?.let { scheduler.executeSync { handleMovePlayer(ctx, it) } }
    DX_BLOCK_UPDATE -> packet.updateBlockOrNull?.let { scheduler.executeAsync { asyncHandleBlockUpdate(ctx, it) } }
    SB_CHUNK_REQUEST -> packet.chunkRequestOrNull?.let { scheduler.executeAsync { asyncHandleChunkRequest(ctx, it) } }
    SB_ENTITY_REQUEST -> packet.entityRequestOrNull?.let {
      val requestedEntities = ctx.getSharedInformation()?.requestedEntities ?: return
      if (requestedEntities.contains(it.uuid)) return@let
      requestedEntities += it.uuid
      scheduler.executeAsync { asyncHandleEntityRequest(ctx, it, requestedEntities) }
    }

    SB_LOGIN -> packet.loginOrNull?.let { scheduler.executeSync { handleLoginPacket(ctx, it) } }
    SB_CLIENT_WORLD_LOADED -> scheduler.executeAsync { asyncHandleClientsWorldLoaded(ctx) }
    DX_SECRET_EXCHANGE -> packet.secretExchangeOrNull?.let { scheduler.executeSync { handleSecretExchange(ctx, it) } }

    DX_DISCONNECT -> scheduler.executeSync { handleDisconnect(ctx, packet.disconnectOrNull) }
    DX_WORLD_SETTINGS -> packet.worldSettingsOrNull?.let { scheduler.executeSync { handleWorldSettings(ctx, it) } }

    UNRECOGNIZED, null -> ctx.fatal("Unknown packet type received ${packet.type}")
    else -> ctx.fatal("Cannot handle packet of type " + packet.type)
  }
}

// ///////////////////////
// NOT SYNCED HANDLERS  //
// ///////////////////////

private fun handleWorldSettings(ctx: ChannelHandlerContextWrapper, worldSettings: WorldSettings) {
  Main.logger().log("handleWorldSettings: spawn? ${worldSettings.hasSpawn()}, time? ${worldSettings.hasTime()}, time scale? ${worldSettings.hasTimeScale()}")
  val world = ServerMain.inst().serverWorld
  var spawn: Long? = null
  var time: Float? = null
  var timeScale: Float? = null

  if (worldSettings.hasSpawn()) {
    world.spawn = worldSettings.spawn.toCompact()
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
  broadcast(clientBoundWorldSettings(spawn, time, timeScale)) { c -> c != ctx.channel() }
}

private fun handleMovePlayer(ctx: ChannelHandlerContextWrapper, moveEntity: MoveEntity) {
  val player = ctx.getCurrentPlayer()
  if (player == null) {
    ctx.fatal("No server side player found!")
    return
  }
  if (player.id != moveEntity.uuid) {
    ctx.fatal("Client tried to update someone else")
    return
  }
  player.teleport(moveEntity.position.x, moveEntity.position.y)
  player.setVelocity(moveEntity.velocity.x, moveEntity.velocity.y)

  // Only set look direction if it exists on the entity from before
  moveEntity.lookDirectionOrNull?.let { player.lookDirectionComponentOrNull?.direction = Direction.valueOf(it) }
}

private fun handleSecretExchange(ctx: ChannelHandlerContextWrapper, secretExchange: SecretExchange) {
  val shared = ctx.getSharedInformation()
  if (shared == null) {
    ctx.fatal("No secret with this channel, send login request first")
    return
  }

  try {
    val uuid = secretExchange.entityUUID
    if (uuid != shared.entityUUID || secretExchange.secret != shared.secret) {
      ctx.fatal("Wrong shared information returned by client")
      return
    }
    val player = Main.inst().world?.getEntity(shared.entityUUID)
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

private fun handleLoginPacket(ctx: ChannelHandlerContextWrapper, login: Packets.Login) {
  val version = Util.getVersion()
  if (login.version != version) {
    ctx.fatal("Version mismatch! Client: '${login.version}' Server: '$version'")
    return
  }
  val uuid = login.uuid
  val username = login.username
  Main.logger().debug("LOGIN", "Login request received by $username uuid $uuid")

  val world = ServerMain.inst().serverWorld
  if (uuid == null) {
    Main.logger().error("handleLoginPacket", "Given player id was null")
    return
  }

  if (world.hasPlayer(uuid)) {
    ctx.writeAndFlush(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.ALREADY_LOGGED_IN))
    ctx.close()
    return
  }
  // Client is good to login (informational packet)
  ctx.writeAndFlush(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.PROCEED_LOGIN))

  val secret = UUID.nameUUIDFromBytes(ByteArray(128).also { secureRandom.nextBytes(it) })
  val sharedInformation = SharedInformation(uuid, secret.toString())
  ServerBoundHandler.clients[ctx.channel()] = sharedInformation

  WorldLoader.spawnServerPlayer(world, uuid, username, sharedInformation)
    .orTimeout(10, TimeUnit.SECONDS)
    .whenComplete { _, ex ->
      if (ex == null) {
        // Exchange the UUID and secret, which will be used to verify the sender, kinda like a bearer bond.
        ctx.writeAndFlush(clientBoundSecretExchange(sharedInformation))
        Main.logger().debug("LOGIN", "Secret sent to player ${sharedInformation.entityUUID}, waiting for confirmation")
      } else {
        ctx.fatal("Failed to spawn player ${sharedInformation.entityUUID} server side.\n  ${ex::class.simpleName}: ${ex.message}")
      }
    }
}

private fun handleHeartbeat(ctx: ChannelHandlerContextWrapper) {
  ctx.getSharedInformation()?.beat() ?: Main.logger().error("handleHeartbeat", "Failed to beat, because of null shared information")
}

private fun handleDisconnect(ctx: ChannelHandlerContextWrapper, disconnect: Disconnect?) {
  Main.logger().log("Client sent disconnect packet. Reason: ${disconnect?.reason ?: "No reason given"}")
  ctx.close()
}

// //////////////////
// ASYNC HANDLERS  //
// //////////////////

private fun asyncHandleBlockUpdate(ctx: ChannelHandlerContextWrapper, blockUpdate: UpdateBlock) {
  val worldX = blockUpdate.pos.x
  val worldY = blockUpdate.pos.y
  if (isLocInView(ctx, worldX, worldY)) {
    ServerMain.inst().serverWorld.setBlock(worldX, worldY, blockUpdate.blockOrNull, true)
  }
}

private fun asyncHandleChunkRequest(ctx: ChannelHandlerContextWrapper, chunkRequest: ChunkRequest) {
  val chunkLoc = chunkRequest.chunkLocation
  val serverWorld = ServerMain.inst().serverWorld

  // Only send chunks which the player is allowed to see
  if (isChunkInView(ctx, chunkLoc.x, chunkLoc.y)) {
    val chunk = serverWorld.getChunk(chunkLoc.x, chunkLoc.y, true) ?: return // if no chunk, don't send a chunk update
    ctx.writeAndFlush(clientBoundUpdateChunkPacket(chunk))
  }
}

private fun asyncHandleClientsWorldLoaded(ctx: ChannelHandlerContext) {
  val world = ServerMain.inst().serverWorld
  val shared = ctx.getSharedInformation()
  val player = ctx.getCurrentPlayer()
  if (player == null || shared == null) {
    ctx.fatal("Player not loaded server-side")
    return
  }

  Main.logger().debug("LOGIN", "Client world ready, sending chunks to client ${player.nameComponent}")

  // Send chunk packets to client
  val ix = player.positionComponent.blockX.worldToChunk()
  val iy = player.positionComponent.blockY.worldToChunk()
  for (cx in -Settings.viewDistance..Settings.viewDistance) {
    for (cy in -Settings.viewDistance..Settings.viewDistance) {
      val chunk = world.getChunk(ix + cx, iy + cy, true) ?: continue
      ctx.write(clientBoundUpdateChunkPacket(chunk))
    }
    ctx.flush()
    Main.logger().debug("LOGIN") {
      val sent = (cx + Settings.viewDistance + 1) * (Settings.viewDistance * 2 + 1)
      val total = (Settings.viewDistance + Settings.viewDistance + 1) * (Settings.viewDistance + Settings.viewDistance + 1)
      "Sent $sent/$total chunks sent to player ${player.name}"
    }
  }
  ctx.writeAndFlush(clientBoundPacketBuilder(CB_INITIAL_CHUNKS_SENT).build())
  Main.logger().debug("LOGIN", "Initial chunks sent to player ${player.name}")

  for (entity in world.validEntitiesToSendToClient) {
    if (entity.id == shared.entityUUID) continue // don't send the player to themselves
    Main.logger().debug("LOGIN") { "Sending entity ${entity.nameOrNull ?: "<unnamed>"} id ${entity.id} to client. ${entity.toComponentsString()}" }
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
  Main.logger().log("Player ${player.name} joined")
}

private fun asyncHandleEntityRequest(ctx: ChannelHandlerContextWrapper, entityRequest: EntityRequest, requestedEntities: ConcurrentHashMap.KeySetView<String, Boolean>) {
  val world = ServerMain.inst().serverWorld
  val uuid = entityRequest.uuid

  val entity = world.getEntity(uuid)
  if (entity != null && entity.shouldSendToClients && isLocInView(ctx, entity.positionComponent.x.toInt(), entity.positionComponent.y.toInt())) {
    ctx.writeAndFlush(clientBoundSpawnEntity(entity))
  } else {
    ctx.writeAndFlush(clientBoundDespawnEntity(uuid, UNKNOWN_ENTITY))
  }
  requestedEntities -= uuid
}

// ///////////
//  UTILS  //
// ///////////

private fun ChannelHandlerContext.getSharedInformation(): SharedInformation? {
  return ServerBoundHandler.clients[this.channel()]
}

private fun ChannelHandlerContext.getCurrentPlayer(): Entity? {
  val uuid = getSharedInformation()?.entityUUID ?: return null
  return ServerMain.inst().serverWorld.getEntity(uuid)
}

/**
 * Use to check if something should be sent to a client
 */
private fun chunksInView(ctx: ChannelHandlerContextWrapper): ChunksInView? {
  val serverWorld = ServerMain.inst().serverWorld
  val uuid = ctx.getSharedInformation()?.entityUUID
  if (uuid == null) {
    Main.logger().error("handleChunkRequest", "Failed to get UUID of requesting entity")
    return null
  }
  val chunksInView = serverWorld.render.getClient(uuid)
  if (chunksInView == null) {
    Main.logger().error("handleChunkRequest", "Failed to get chunks in view of entity $uuid")
    return null
  }
  return chunksInView
}

private fun isChunkInView(ctx: ChannelHandlerContextWrapper, chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean {
  return chunksInView(ctx)?.isInView(chunkX, chunkY) ?: false
}

private fun isLocInView(ctx: ChannelHandlerContextWrapper, worldX: WorldCoord, worldY: WorldCoord): Boolean {
  return chunksInView(ctx)?.isInView(worldX.worldToChunk(), worldY.worldToChunk()) ?: false
}
