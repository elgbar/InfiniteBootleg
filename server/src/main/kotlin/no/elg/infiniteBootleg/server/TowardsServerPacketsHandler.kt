package no.elg.infiniteBootleg.server

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.console.logPacket
import no.elg.infiniteBootleg.console.serverSideServerBoundMarker
import no.elg.infiniteBootleg.console.temporallyFilterPacket
import no.elg.infiniteBootleg.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.inventory.container.ContainerOwner.Companion.fromProto
import no.elg.infiniteBootleg.inventory.container.OwnedContainer.Companion.fromProto
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.main.ServerMain
import no.elg.infiniteBootleg.protobuf.Packets
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
import no.elg.infiniteBootleg.protobuf.updateSelectedSlotOrNull
import no.elg.infiniteBootleg.protobuf.worldSettingsOrNull
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.generateUUIDFromString
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.util.launchOnMain
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.util.toCompact
import no.elg.infiniteBootleg.util.toComponentsString
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.Staff
import no.elg.infiniteBootleg.world.ecs.components.InputEventQueueComponent.Companion.inputEventQueueOrNull
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.name
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.setVelocity
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent.Companion.hotbarComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.shouldSendToClients
import no.elg.infiniteBootleg.world.loader.ServerWorldLoader
import no.elg.infiniteBootleg.world.render.ChunksInView
import no.elg.infiniteBootleg.world.render.ChunksInView.Companion.forEach
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit

private val secureRandom = SecureRandom.getInstanceStrong()
private val logger = KotlinLogging.logger {}

/**
 * Handle packets sent TO the server FROM a client, will quietly drop any packets that are malformed
 * @author Elg
 */
fun handleServerBoundPackets(ctx: ChannelHandlerContextWrapper, packet: Packets.Packet) {
  logPacket(serverSideServerBoundMarker, packet)
  when (packet.type) {
    Packets.Packet.Type.DX_HEARTBEAT -> handleHeartbeat(ctx)
    Packets.Packet.Type.DX_MOVE_ENTITY -> packet.moveEntityOrNull?.let { launchOnMain { handleMovePlayer(ctx, it) } }
    Packets.Packet.Type.DX_BLOCK_UPDATE -> packet.updateBlockOrNull?.let {
      launchOnAsync {
        asyncHandleBlockUpdate(
          ctx,
          it
        )
      }
    }

    Packets.Packet.Type.SB_CONTENT_REQUEST -> packet.contentRequestOrNull?.let { handleContentRequest(ctx, it) }

    Packets.Packet.Type.DX_BREAKING_BLOCK -> packet.breakingBlockOrNull?.let {
      launchOnAsync {
        asyncHandleBreakingBlock(
          ctx,
          it
        )
      }
    }

    Packets.Packet.Type.DX_CONTAINER_UPDATE -> packet.containerUpdateOrNull?.let {
      launchOnAsync {
        asyncHandleContainerUpdate(
          ctx,
          it
        )
      }
    }

    Packets.Packet.Type.SB_SELECT_SLOT -> packet.updateSelectedSlotOrNull?.let {
      launchOnAsync {
        asyncHandleUpdateSelectedSlot(
          ctx,
          it
        )
      }
    }

    Packets.Packet.Type.SB_LOGIN -> packet.loginOrNull?.let { launchOnMain { handleLoginPacket(ctx, it) } }
    Packets.Packet.Type.SB_CLIENT_WORLD_LOADED -> launchOnAsync { asyncHandleClientsWorldLoaded(ctx) }
    Packets.Packet.Type.DX_SECRET_EXCHANGE -> packet.secretExchangeOrNull?.let {
      launchOnMain {
        handleSecretExchange(
          ctx,
          it
        )
      }
    }

    Packets.Packet.Type.DX_DISCONNECT -> launchOnMain { handleDisconnect(ctx, packet.disconnectOrNull) }
    Packets.Packet.Type.DX_WORLD_SETTINGS -> packet.worldSettingsOrNull?.let {
      launchOnMain {
        handleWorldSettings(
          ctx,
          it
        )
      }
    }

    Packets.Packet.Type.SB_CAST_SPELL -> launchOnAsync { asyncHandleCastSpell(ctx) }

    Packets.Packet.Type.UNRECOGNIZED -> ctx.fatal("Unknown packet type received by server: ${packet.type}")
    else -> ctx.fatal("Server cannot handle packet of type ${packet.type}")
  }
}
// ///////////////////
// SYNCED HANDLERS  //
// ///////////////////

private fun handleContentRequest(ctx: ChannelHandlerContextWrapper, contentRequest: Packets.ContentRequest) {
  // Chunk request
  contentRequest.chunkLocationOrNull?.let { launchOnAsync { asyncHandleChunkRequest(ctx, it.x, it.y) } }
  // Entity request
  if (contentRequest.hasEntityRef()) {
    val entityId: String = contentRequest.entityRef.id
    val requestedEntities = ctx.getSharedInformation()?.requestedEntities ?: return
    requestedEntities.get(entityId) {
      launchOnAsync { asyncHandleEntityRequest(ctx, entityId) }
    }
  }
  // Container
  contentRequest.containerOwner?.let { owner: ProtoWorld.ContainerOwner ->
    launchOnAsync {
      asyncHandleContainerRequest(ctx, owner.fromProto() ?: return@launchOnAsync)
    }
  }
}

private fun handleWorldSettings(ctx: ChannelHandlerContextWrapper, worldSettings: Packets.WorldSettings) {
  logger.info { "handleWorldSettings: spawn? ${worldSettings.hasSpawn()}, time? ${worldSettings.hasTime()}, time scale? ${worldSettings.hasTimeScale()}" }
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
  Main.inst().packetBroadcaster.broadcast(clientBoundWorldSettings(spawn, time, timeScale)) { c -> c != ctx.channel() }
}

private fun handleMovePlayer(ctx: ChannelHandlerContextWrapper, moveEntity: Packets.MoveEntity) {
  val player = ctx.getCurrentPlayer()
  if (player == null) {
    ctx.fatal("No server side player found!")
    return
  }
  if (player.id != moveEntity.ref.id) {
    ctx.fatal("Client tried to update someone else")
    return
  }
  player.teleport(moveEntity.position.x, moveEntity.position.y)
  player.setVelocity(moveEntity.velocity.x, moveEntity.velocity.y)

  // Only set look direction if it exists on the entity from before
  moveEntity.lookDirectionOrNull?.let { player.lookDirectionComponentOrNull?.direction = Direction.Companion.valueOf(it) }
}

private fun handleSecretExchange(ctx: ChannelHandlerContextWrapper, secretExchange: Packets.SecretExchange) {
  val shared = ctx.getSharedInformation()
  if (shared == null) {
    ctx.fatal("No secret with this channel, send login request first")
    return
  }

  try {
    val id = secretExchange.ref.id
    if (id != shared.entityId || secretExchange.secret != shared.secret) {
      ctx.fatal("Wrong shared information returned by client")
      return
    }
    val player = Main.Companion.inst().world?.getEntity(shared.entityId)
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
  val version = Util.version
  if (login.version != version) {
    ctx.fatal("Version mismatch! Client: '${login.version}' Server: '$version'")
    return
  }
  val username = login.username
  val entityId = generateUUIDFromString(username).toString()
  if (username.isBlank()) {
    ctx.fatal("Blank usernames are not allowed")
    return
  }
  logger.debug { "Login request received by user '$username', entityId '$entityId'" }

  val world = ServerMain.inst().serverWorld

  if (world.hasPlayer(entityId)) {
    ctx.writeAndFlushPacket(clientBoundLoginStatusPacket(Packets.ServerLoginStatus.ServerStatus.ALREADY_LOGGED_IN))
    ctx.close()
    return
  }
  // Client is good to login (informational packet)
  ctx.writeAndFlushPacket(clientBoundLoginStatusPacket(Packets.ServerLoginStatus.ServerStatus.PROCEED_LOGIN))

  val secret = UUID.nameUUIDFromBytes(ByteArray(128).also { secureRandom.nextBytes(it) })
  val sharedInformation = SharedInformation(entityId, secret.toString())
  ServerBoundHandler.clients[ctx.channel()] = sharedInformation

  ServerWorldLoader.spawnServerPlayer(world, entityId, username, sharedInformation)
    .orTimeout(10, TimeUnit.SECONDS)
    .whenComplete { _, ex ->
      if (ex == null) {
        // Exchange the UUID and secret, which will be used to verify the sender, kinda like a bearer bond.
        ctx.writeAndFlushPacket(clientBoundSecretExchange(sharedInformation))
        logger.debug { "Secret sent to player ${sharedInformation.entityId}, waiting for confirmation" }
      } else {
        ctx.fatal("Failed to spawn player ${sharedInformation.entityId} server side.\n  ${ex::class.simpleName}: ${ex.message}")
      }
    }
}

private fun handleHeartbeat(ctx: ChannelHandlerContextWrapper) {
  ctx.getSharedInformation()?.beat() ?: logger.error { "Failed to beat, because of null shared information" }
}

private fun handleDisconnect(ctx: ChannelHandlerContextWrapper, disconnect: Packets.Disconnect?) {
  logger.info { "Client sent disconnect packet. Reason: ${disconnect?.reason ?: "No reason given"}" }
  ctx.close()
}

// //////////////////
// ASYNC HANDLERS  //
// //////////////////

private fun asyncHandleBlockUpdate(ctx: ChannelHandlerContextWrapper, blockUpdate: Packets.UpdateBlock) {
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
  } else {
    logger.debug { "Client request chunk out side of its view ${stringifyCompactLoc(chunkX, chunkY)}" }
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

  logger.debug { "Client world ready, sending chunks to client ${player.nameOrNull}" }

  // Send chunk packets to client
  val chunksInView = chunksInView(ctx) ?: run {
    ctx.fatal("Failed to find chunks in view, serverside")
    return
  }
  temporallyFilterPacket(Packets.Packet.Type.CB_UPDATE_CHUNK) {
    chunksInView.forEach(world) {
      ctx.writePacket(clientBoundUpdateChunkPacket(it))
    }
    ctx.flush()
  }
  ctx.writeAndFlushPacket(clientBoundPacketBuilder(Packets.Packet.Type.CB_INITIAL_CHUNKS_SENT).build())
  logger.debug { "Initial ${chunksInView.size} chunks sent to player ${player.name}" }

  val validEntitiesToSendToClient = world.validEntitiesToSendToClient
    .filterNot { it.id == shared.entityId } // don't send the player to themselves
    .filter {
      val pos = it.position
      isLocInView(ctx, pos.x.toInt(), pos.y.toInt())
    }
  if (validEntitiesToSendToClient.isNotEmpty()) {
    for (entity in validEntitiesToSendToClient) {
      logger.debug { "Sending entity ${entity.nameOrNull ?: "<unnamed>"} id ${entity.id} to client. ${entity.toComponentsString()}" }
      ctx.writePacket(clientBoundSpawnEntity(entity))
    }
    ctx.flush()
    logger.debug { "Initial entities sent to player ${player.name}" }
  } else {
    logger.debug { "No entities to send to player ${player.name}" }
  }

  ctx.writeAndFlushPacket(clientBoundLoginStatusPacket(Packets.ServerLoginStatus.ServerStatus.LOGIN_SUCCESS))

  shared.heartbeatTask = ctx.executor().scheduleAtFixedRate({
//    logger.info { "Sending heartbeat to client" }
    ctx.writeAndFlushPacket(clientBoundHeartbeat())
    if (shared.lostConnection()) {
      logger.error { "Client stopped responding, heartbeats not received" }
      ctx.close()
      ctx.deregister()
      ctx.channel().close()
      ctx.channel().disconnect()
    }
  }, SharedInformation.Companion.HEARTBEAT_PERIOD_MS, SharedInformation.Companion.HEARTBEAT_PERIOD_MS, TimeUnit.MILLISECONDS)
  logger.info { "Player ${player.name} joined" }
}

private fun asyncHandleEntityRequest(ctx: ChannelHandlerContextWrapper, entityId: String) {
  val world = ServerMain.inst().serverWorld

  val entity = world.getEntity(entityId) ?: run {
    ctx.writeAndFlushPacket(clientBoundDespawnEntity(entityId, Packets.DespawnEntity.DespawnReason.UNKNOWN_ENTITY))
    return
  }
  val position = entity.position
  if (entity.shouldSendToClients && isLocInView(ctx, position.x.toInt(), position.y.toInt())) {
    ctx.writeAndFlushPacket(clientBoundSpawnEntity(entity))
  }
}

private fun asyncHandleBreakingBlock(ctx: ChannelHandlerContextWrapper, breakingBlock: Packets.BreakingBlock) {
  // Naive and simple re-broadcast
  Main.inst().packetBroadcaster.broadcast(clientBoundPacketBuilder(Packets.Packet.Type.DX_BREAKING_BLOCK).setBreakingBlock(breakingBlock).build()) { c -> c != ctx.channel() }
}

private fun asyncHandleContainerUpdate(ctx: ChannelHandlerContextWrapper, containerUpdate: Packets.ContainerUpdate) {
  // Naive and simple re-broadcast
  // TODO check that the player can do this
  // FIXME make sure not to broadcast when no changes are made
  val (owner, container) = containerUpdate.worldContainer.fromProto()
  ServerMain.inst().serverWorld.worldContainerManager.find(owner).thenApply { serverOwnedContainer ->
    container.content.copyInto(serverOwnedContainer.container.content)
  }
  Main.inst().packetBroadcaster.broadcast(clientBoundPacketBuilder(Packets.Packet.Type.DX_CONTAINER_UPDATE).setContainerUpdate(containerUpdate).build()) { c -> c != ctx.channel() }
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
      logger.warn { "Failed to find container for $owner" }
    } else {
      ctx.writeAndFlushPacket(clientBoundContainerUpdate(ownedContainer))
    }
  }
}

private fun asyncHandleUpdateSelectedSlot(ctx: ChannelHandlerContextWrapper, updateSelectedSlot: Packets.UpdateSelectedSlot) {
  val entity = ctx.getCurrentPlayer() ?: return
  val slot = HotbarComponent.Companion.HotbarSlot.fromOrdinalOrNull(updateSelectedSlot.slot) ?: return
  val hotbarComponent = entity.hotbarComponentOrNull ?: run {
    logger.debug { "No hotbar component found on player ${entity.nameOrNull}" }
    return
  }
  hotbarComponent.selected = slot
  val selectedElement = hotbarComponent.selectedItem(entity)?.element ?: Material.AIR
  Main.inst().packetBroadcaster.broadcast(clientBoundHoldingItem(entity, selectedElement)) { c -> c != ctx.channel() }
}

// ///////////
//  UTILS  //
// ///////////

private fun ChannelHandlerContextWrapper.getSharedInformation(): SharedInformation? {
  return ServerBoundHandler.clients[this.channel()]
}

private fun ChannelHandlerContextWrapper.getCurrentPlayer(): Entity? {
  val uuid = getSharedInformation()?.entityId ?: return null
  return ServerMain.inst().serverWorld.getEntity(uuid)
}

/**
 * Use to check if something should be sent to a client
 */
private fun chunksInView(ctx: ChannelHandlerContextWrapper): ChunksInView? {
  val serverWorld = ServerMain.inst().serverWorld
  val uuid = ctx.getSharedInformation()?.entityId
  if (uuid == null) {
    logger.error { "Failed to get UUID of requesting entity" }
    return null
  }
  val chunksInView = serverWorld.render.getClient(uuid)
  if (chunksInView == null) {
    logger.error { "Failed to get chunks in view of entity $uuid" }
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
