package no.elg.infiniteBootleg.server

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.console.logPacket
import no.elg.infiniteBootleg.console.temporallyFilterPacket
import no.elg.infiniteBootleg.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.inventory.container.ContainerOwner.Companion.fromProto
import no.elg.infiniteBootleg.inventory.container.OwnedContainer.Companion.fromProto
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.main.ServerMain
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.BreakingBlock
import no.elg.infiniteBootleg.protobuf.Packets.ContainerUpdate
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.UNKNOWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Disconnect
import no.elg.infiniteBootleg.protobuf.Packets.MoveEntity
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_INITIAL_CHUNKS_SENT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BLOCK_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BREAKING_BLOCK
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_CONTAINER_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_DISCONNECT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_HEARTBEAT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_MOVE_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_SECRET_EXCHANGE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_WORLD_SETTINGS
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CAST_SPELL
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CLIENT_WORLD_LOADED
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CONTENT_REQUEST
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_LOGIN
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.UNRECOGNIZED
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock
import no.elg.infiniteBootleg.protobuf.Packets.WorldSettings
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.blockOrNull
import no.elg.infiniteBootleg.protobuf.breakingBlockOrNull
import no.elg.infiniteBootleg.protobuf.chunkLocationOrNull
import no.elg.infiniteBootleg.protobuf.containerUpdateOrNull
import no.elg.infiniteBootleg.protobuf.contentRequestOrNull
import no.elg.infiniteBootleg.protobuf.disconnectOrNull
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
import no.elg.infiniteBootleg.world.Staff
import no.elg.infiniteBootleg.world.ecs.components.InputEventQueueComponent.Companion.inputEventQueueOrNull
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.name
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameComponent
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.shouldSendToClients
import no.elg.infiniteBootleg.world.loader.WorldLoader
import no.elg.infiniteBootleg.world.render.ChunksInView
import java.security.SecureRandom
import java.util.UUID
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

    SB_CONTENT_REQUEST -> packet.contentRequestOrNull?.let { contentRequest: Packets.ContentRequest ->
      // Chunk request
      contentRequest.chunkLocationOrNull?.let { scheduler.executeAsync { asyncHandleChunkRequest(ctx, it.x, it.y) } }
      // Entity request
      if (contentRequest.hasEntityUUID()) {
        val entityUUID: String = contentRequest.entityUUID
        val requestedEntities = ctx.getSharedInformation()?.requestedEntities ?: return
        requestedEntities.get(entityUUID) {
          scheduler.executeAsync { asyncHandleEntityRequest(ctx, entityUUID) }
        }
      }
      // Container
      contentRequest.containerOwner?.let { owner: ProtoWorld.ContainerOwner ->
        scheduler.executeAsync {
          asyncHandleContainerRequest(ctx, owner.fromProto() ?: return@executeAsync)
        }
      }
    }

    DX_BREAKING_BLOCK -> packet.breakingBlockOrNull?.let { scheduler.executeAsync { asyncHandleBreakingBlock(ctx, it) } }
    DX_CONTAINER_UPDATE -> packet.containerUpdateOrNull?.let { scheduler.executeAsync { asyncHandleContainerUpdate(ctx, it) } }

    SB_LOGIN -> packet.loginOrNull?.let { scheduler.executeSync { handleLoginPacket(ctx, it) } }
    SB_CLIENT_WORLD_LOADED -> scheduler.executeAsync { asyncHandleClientsWorldLoaded(ctx) }
    DX_SECRET_EXCHANGE -> packet.secretExchangeOrNull?.let { scheduler.executeSync { handleSecretExchange(ctx, it) } }

    DX_DISCONNECT -> scheduler.executeSync { handleDisconnect(ctx, packet.disconnectOrNull) }
    DX_WORLD_SETTINGS -> packet.worldSettingsOrNull?.let { scheduler.executeSync { handleWorldSettings(ctx, it) } }

    SB_CAST_SPELL -> scheduler.executeAsync { asyncHandleCastSpell(ctx) }

    UNRECOGNIZED -> ctx.fatal("Unknown packet type received by server: ${packet.type}")
    else -> ctx.fatal("Server cannot handle packet of type ${packet.type}")
  }
}

// ///////////////////
// SYNCED HANDLERS  //
// ///////////////////

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
      ctx.writeAndFlushPacket(clientBoundStartGamePacket(player))
    } else {
      ctx.fatal("handleSecretExchange: Failed to find entity with the given uuid")
    }
  } catch (e: Exception) {
    ctx.fatal("handleSecretExchange: ${e::class} thrown when trying to handle secret exchange")
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
    ctx.writeAndFlushPacket(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.ALREADY_LOGGED_IN))
    ctx.close()
    return
  }
  // Client is good to login (informational packet)
  ctx.writeAndFlushPacket(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.PROCEED_LOGIN))

  val secret = UUID.nameUUIDFromBytes(ByteArray(128).also { secureRandom.nextBytes(it) })
  val sharedInformation = SharedInformation(uuid, secret.toString())
  ServerBoundHandler.clients[ctx.channel()] = sharedInformation

  WorldLoader.spawnServerPlayer(world, uuid, username, sharedInformation)
    .orTimeout(10, TimeUnit.SECONDS)
    .whenComplete { _, ex ->
      if (ex == null) {
        // Exchange the UUID and secret, which will be used to verify the sender, kinda like a bearer bond.
        ctx.writeAndFlushPacket(clientBoundSecretExchange(sharedInformation))
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

private fun asyncHandleChunkRequest(ctx: ChannelHandlerContextWrapper, chunkX: ChunkCoord, chunkY: ChunkCoord) {
  val serverWorld = ServerMain.inst().serverWorld

  // Only send chunks which the player is allowed to see
  if (isChunkInView(ctx, chunkX, chunkY)) {
    val chunk = serverWorld.getChunk(chunkX, chunkY, true) ?: return // if no chunk, don't send a chunk update
    ctx.writeAndFlushPacket(clientBoundUpdateChunkPacket(chunk))
  }
}

private fun asyncHandleClientsWorldLoaded(ctx: ChannelHandlerContextWrapper) {
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
  temporallyFilterPacket(Packets.Packet.Type.CB_UPDATE_CHUNK) {
    for (cx in -Settings.viewDistance..Settings.viewDistance) {
      for (cy in -Settings.viewDistance..Settings.viewDistance) {
        val chunk = try {
          world.getChunk(ix + cx, iy + cy, true) ?: continue
        } catch (e: IllegalStateException) {
          Main.logger().warn("LOGIN", "Failed to get chunk at $cx, $cy")
          continue
        }
        ctx.writePacket(clientBoundUpdateChunkPacket(chunk))
      }
      ctx.flush()
      Main.logger().debug("LOGIN") {
        val sent = (cx + Settings.viewDistance + 1) * (Settings.viewDistance * 2 + 1)
        val total = (Settings.viewDistance + Settings.viewDistance + 1) * (Settings.viewDistance + Settings.viewDistance + 1)
        "Sent $sent/$total chunks sent to player ${player.name}"
      }
    }
  }
  ctx.writeAndFlushPacket(clientBoundPacketBuilder(CB_INITIAL_CHUNKS_SENT).build())
  Main.logger().debug("LOGIN", "Initial chunks sent to player ${player.name}")

  for (entity in world.validEntitiesToSendToClient) {
    if (entity.id == shared.entityUUID) continue // don't send the player to themselves
    Main.logger().debug("LOGIN") { "Sending entity ${entity.nameOrNull ?: "<unnamed>"} id ${entity.id} to client. ${entity.toComponentsString()}" }
    ctx.writePacket(clientBoundSpawnEntity(entity))
  }

  ctx.flush()
  Main.logger().debug("LOGIN", "Initial entities sent to player ${player.name}")

  ctx.writeAndFlushPacket(clientBoundLoginStatusPacket(ServerLoginStatus.ServerStatus.LOGIN_SUCCESS))

  shared.heartbeatTask = ctx.executor().scheduleAtFixedRate({
//    Main.logger().log("Sending heartbeat to client")
    ctx.writeAndFlushPacket(clientBoundHeartbeat())
    if (shared.lostConnection()) {
      Main.logger().error("Heartbeat", "Client stopped responding, heartbeats not received")
      ctx.close()
      ctx.deregister()
      ctx.channel().close()
      ctx.channel().disconnect()
    }
  }, HEARTBEAT_PERIOD_MS, HEARTBEAT_PERIOD_MS, TimeUnit.MILLISECONDS)
  Main.logger().log("Player ${player.name} joined")
}

private fun asyncHandleEntityRequest(ctx: ChannelHandlerContextWrapper, uuid: String) {
  val world = ServerMain.inst().serverWorld

  val entity = world.getEntity(uuid) ?: run {
    ctx.writeAndFlushPacket(clientBoundDespawnEntity(uuid, UNKNOWN_ENTITY))
    return
  }
  val position = entity.position
  if (entity.shouldSendToClients && isLocInView(ctx, position.x.toInt(), position.y.toInt())) {
    ctx.writeAndFlushPacket(clientBoundSpawnEntity(entity))
  }
}

private fun asyncHandleBreakingBlock(ctx: ChannelHandlerContextWrapper, breakingBlock: BreakingBlock) {
  // Naive and simple re-broadcast
  broadcast(clientBoundPacketBuilder(DX_BREAKING_BLOCK).setBreakingBlock(breakingBlock).build()) { c -> c != ctx.channel() }
}

private fun asyncHandleContainerUpdate(ctx: ChannelHandlerContextWrapper, containerUpdate: ContainerUpdate) {
  // Naive and simple re-broadcast
  // TODO check that the player can do this
  val (owner, container) = containerUpdate.worldContainer.fromProto()
  ServerMain.inst().serverWorld.worldContainerManager.find(owner).thenApply { serverOwnedContainer ->
    container.content.copyInto(serverOwnedContainer.container.content)
  }
  broadcast(clientBoundPacketBuilder(DX_CONTAINER_UPDATE).setContainerUpdate(containerUpdate).build()) // { c -> c != ctx.channel() }
}

private fun asyncHandleCastSpell(ctx: ChannelHandlerContextWrapper) {
  val player = ctx.getCurrentPlayer() ?: return
  val staff = player.selectedItem?.element as? Staff ?: return
  val inputEventQueue = player.inputEventQueueOrNull ?: return
  inputEventQueue.events += InputEvent.SpellCastEvent(staff)
}

private fun asyncHandleContainerRequest(ctx: ChannelHandlerContextWrapper, owner: ContainerOwner) {
  ServerMain.inst().serverWorld.worldContainerManager.find(owner).thenApply { ownedContainer ->
    if (ownedContainer == null) {
      Main.logger().warn("asyncHandleContainerRequest", "Failed to find container for $owner")
    } else {
      ctx.writeAndFlushPacket(clientBoundContainerUpdate(ownedContainer))
    }
  }
}

// ///////////
//  UTILS  //
// ///////////

private fun ChannelHandlerContextWrapper.getSharedInformation(): SharedInformation? {
  return ServerBoundHandler.clients[this.channel()]
}

private fun ChannelHandlerContextWrapper.getCurrentPlayer(): Entity? {
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
