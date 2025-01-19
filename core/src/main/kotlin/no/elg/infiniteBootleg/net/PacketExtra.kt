package no.elg.infiniteBootleg.net

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.inventory.container.OwnedContainer.Companion.asProto
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ContentRequestKt
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.protobuf.Packets.Disconnect
import no.elg.infiniteBootleg.protobuf.Packets.Heartbeat
import no.elg.infiniteBootleg.protobuf.Packets.InterfaceUpdate
import no.elg.infiniteBootleg.protobuf.Packets.MoveEntity
import no.elg.infiniteBootleg.protobuf.Packets.Packet
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Direction.CLIENT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Direction.SERVER
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_DESPAWN_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_HOLDING_ITEM
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_INTERFACE_UPDATE
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
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CAST_SPELL
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CONTENT_REQUEST
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_LOGIN
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_SELECT_SLOT
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.protobuf.Packets.SpawnEntity
import no.elg.infiniteBootleg.protobuf.Packets.StartGame
import no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock
import no.elg.infiniteBootleg.protobuf.Packets.UpdateChunk
import no.elg.infiniteBootleg.protobuf.Packets.WorldSettings
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
import no.elg.infiniteBootleg.protobuf.containerUpdate
import no.elg.infiniteBootleg.protobuf.contentRequest
import no.elg.infiniteBootleg.protobuf.holdingItem
import no.elg.infiniteBootleg.protobuf.interfaceUpdate
import no.elg.infiniteBootleg.protobuf.moveEntity
import no.elg.infiniteBootleg.protobuf.updateSelectedSlot
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.decompactLocX
import no.elg.infiniteBootleg.util.decompactLocY
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.util.launchOnMain
import no.elg.infiniteBootleg.util.toComponentsString
import no.elg.infiniteBootleg.util.toProtoEntityRef
import no.elg.infiniteBootleg.util.toVector2i
import no.elg.infiniteBootleg.world.ContainerElement
import no.elg.infiniteBootleg.world.ContainerElement.Companion.asProto
import no.elg.infiniteBootleg.world.Material.AIR
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.world.ecs.save
import java.time.Instant

private val logger = KotlinLogging.logger {}
// //////////////////
// util functions //
// //////////////////

internal fun ChannelHandlerContextWrapper.fatal(msg: String) {
  require(Settings.client)
  launchOnAsync {
    delay(50L)
    close()
  }
  launchOnMain {
    ConnectingScreen.info = msg
    ClientMain.inst().screen = ConnectingScreen
    val serverClient = ClientMain.inst().serverClient
    if (serverClient?.sharedInformation != null) {
      this@fatal.writeAndFlushPacket(serverClient.serverBoundClientDisconnectPacket(msg))
    }
  }
  logger.error { msg }
}

fun ServerClient.serverBoundPacketBuilder(type: Type): Packet.Builder {
  return Packet.newBuilder()
    .setDirection(SERVER)
    .setSecret(sharedInformation!!.secret) // FIXME
    .setType(type)
}

fun clientBoundPacketBuilder(type: Type): Packet.Builder {
  return Packet.newBuilder()
    .setDirection(CLIENT)
    .setType(type)
}

fun entityMovePacket(entity: Entity): MoveEntity {
  return moveEntity {
    ref = entity.toProtoEntityRef()
    position = entity.positionComponent.toProtoVector2f()
    velocity = entity.velocityComponent.toProtoVector2f()
    entity.lookDirectionComponentOrNull?.direction?.toProtoVector2i()?.let { lookDirection = it }
  }
}

// ////////////////
// server bound //
// ////////////////

fun serverBoundLoginPacket(name: String): Packet {
  return Packet.newBuilder()
    .setDirection(SERVER)
    .setType(SB_LOGIN)
    .setLogin(
      Packets.Login.newBuilder()
        .setUsername(name)
        .setVersion(Util.version)
    ).build()
}

fun ServerClient.serverBoundBlockUpdate(worldX: WorldCoord, worldY: WorldCoord, block: Block?): Packet {
  return serverBoundPacketBuilder(DX_BLOCK_UPDATE).setUpdateBlock(
    UpdateBlock.newBuilder()
      .setPos(Vector2i.newBuilder().setX(worldX).setY(worldY))
      .setBlock(block?.save() ?: PROTO_AIR_BLOCK)
  ).build()
}

fun ServerClient.serverBoundClientSecretResponse(sharedInformation: SharedInformation): Packet {
  return serverBoundPacketBuilder(DX_SECRET_EXCHANGE).setSecretExchange(
    SecretExchange.newBuilder()
      .setSecret(sharedInformation.secret)
      .setRef(sharedInformation.entityId.toProtoEntityRef())
  ).build()
}

fun ServerClient.serverBoundChunkRequestPacket(x: ChunkCoord, y: ChunkCoord): Packet {
  return serverBoundChunkRequestPacket(Vector2i.newBuilder().setX(x).setY(y).build())
}

private fun ServerClient.serverBoundContentRequest(block: ContentRequestKt.Dsl.() -> Unit): Packet =
  serverBoundPacketBuilder(SB_CONTENT_REQUEST).setContentRequest(contentRequest(block)).build()

fun ServerClient.serverBoundChunkRequestPacket(chunkPos: Vector2i): Packet =
  serverBoundContentRequest {
    chunkLocation = chunkPos
  }

fun ServerClient.serverBoundEntityRequest(entityId: String): Packet =
  serverBoundContentRequest {
    entityRef = entityId.toProtoEntityRef()
  }

fun ServerClient.serverBoundContainerRequest(owner: ProtoWorld.ContainerOwner): Packet =
  serverBoundContentRequest {
    containerOwner = owner
  }

fun ServerClient.serverBoundClientDisconnectPacket(reason: String? = null): Packet {
  return serverBoundPacketBuilder(DX_DISCONNECT).let {
    if (!reason.isNullOrBlank()) {
      it.setDisconnect(Disconnect.newBuilder().setReason(reason))
    }
    it.build()
  }
}

fun ServerClient.serverBoundMoveEntityPacket(entity: Entity): Packet {
  return serverBoundPacketBuilder(DX_MOVE_ENTITY)
    .setMoveEntity(entityMovePacket(entity))
    .build()
}

fun ServerClient.serverBoundWorldSettings(spawn: Long?, time: Float?, timeScale: Float?): Packet {
  return worldSettingsPacketBuilder(serverBoundPacketBuilder(DX_WORLD_SETTINGS), spawn, time, timeScale)
}

fun ServerClient.serverBoundHeartbeat(): Packet {
  return heartbeatPacketBuilder(serverBoundPacketBuilder(DX_HEARTBEAT))
}

fun ServerClient.serverBoundContainerUpdate(owner: ContainerOwner, container: Container): Packet = serverBoundContainerUpdate(OwnedContainer(owner, container))
fun ServerClient.serverBoundContainerUpdate(ownedContainer: OwnedContainer): Packet = containerUpdateBuilder(serverBoundPacketBuilder(DX_CONTAINER_UPDATE), ownedContainer)

fun ServerClient.serverBoundBreakingBlock(progress: List<Packets.BreakingBlock.BreakingProgress>): Packet {
  return serverBoundPacketBuilder(DX_BREAKING_BLOCK)
    .setBreakingBlock(Packets.BreakingBlock.newBuilder().addAllBreakingProgress(progress))
    .build()
}

fun ServerClient.serverBoundSpellSpawn(): Packet {
  return serverBoundPacketBuilder(SB_CAST_SPELL).build()
}

fun ServerClient.serverBoundUpdateSelectedSlot(slot: HotbarComponent.Companion.HotbarSlot): Packet {
  return serverBoundPacketBuilder(SB_SELECT_SLOT)
    .setUpdateSelectedSlot(updateSelectedSlot { this.slot = slot.ordinal })
    .build()
}

// ////////////////
// client bound //
// ////////////////

private val PROTO_AIR_BLOCK = Block.save(AIR).build()

fun clientBoundBlockUpdate(worldX: WorldCoord, worldY: WorldCoord, block: Block?): Packet {
  return clientBoundPacketBuilder(DX_BLOCK_UPDATE).setUpdateBlock(
    UpdateBlock.newBuilder()
      .setBlock(block?.save() ?: PROTO_AIR_BLOCK)
      .setPos(Vector2i.newBuilder().setX(worldX).setY(worldY))
  ).build()
}

fun clientBoundMoveEntity(entity: Entity): Packet {
  return clientBoundPacketBuilder(DX_MOVE_ENTITY)
    .setMoveEntity(entityMovePacket(entity))
    .build()
}

fun clientBoundHoldingItem(entity: Entity, element: ContainerElement): Packet {
  return clientBoundPacketBuilder(CB_HOLDING_ITEM)
    .setHoldingItem(
      holdingItem {
        this.element = element.asProto()
        this.entityRef = entity.toProtoEntityRef()
      }
    )
    .build()
}

fun clientBoundSpawnEntity(entity: Entity): Packet {
  if (entity.authoritativeOnly) {
    throw IllegalStateException("Cannot send entity with the tag authoritative only to clients ${entity.toComponentsString()}")
  }
  return clientBoundPacketBuilder(CB_SPAWN_ENTITY).setSpawnEntity(
    SpawnEntity.newBuilder()
      .setEntity(entity.save(toAuthoritative = false, ignoreTransient = true))
      .setRef(entity.toProtoEntityRef())
  ).build()
}

fun clientBoundDespawnEntity(entityId: String, reason: DespawnReason): Packet {
  return clientBoundPacketBuilder(CB_DESPAWN_ENTITY).setDespawnEntity(
    DespawnEntity.newBuilder()
      .setRef(entityId.toProtoEntityRef())
      .setDespawnReason(reason)
  ).build()
}

fun clientBoundLoginStatusPacket(status: ServerLoginStatus.ServerStatus): Packet {
  return clientBoundPacketBuilder(CB_LOGIN_STATUS).setServerLoginStatus(ServerLoginStatus.newBuilder().setStatus(status)).build()
}

fun clientBoundStartGamePacket(player: Entity): Packet {
  return clientBoundPacketBuilder(CB_START_GAME).setStartGame(
    StartGame.newBuilder()
      .setWorld(player.world.toProtobuf())
      .setControlling(player.save(toAuthoritative = true, ignoreTransient = true))
  ).build()
}

fun clientBoundUpdateChunkPacket(chunk: Chunk): Packet {
  return clientBoundPacketBuilder(CB_UPDATE_CHUNK).setUpdateChunk(UpdateChunk.newBuilder().setChunk(chunk.saveBlocksOnly())).build()
}

fun clientBoundDisconnectPlayerPacket(reason: String?): Packet {
  return clientBoundPacketBuilder(DX_DISCONNECT).let {
    if (!reason.isNullOrBlank()) {
      it.setDisconnect(Disconnect.newBuilder().setReason(reason))
    }
    it.build()
  }
}

fun clientBoundSecretExchange(sharedInformation: SharedInformation): Packet {
  return clientBoundPacketBuilder(DX_SECRET_EXCHANGE).setSecretExchange(
    SecretExchange.newBuilder()
      .setSecret(sharedInformation.secret)
      .setRef(sharedInformation.entityId.toProtoEntityRef())
  ).build()
}

fun clientBoundWorldSettings(spawn: Long?, time: Float?, timeScale: Float?): Packet {
  return worldSettingsPacketBuilder(clientBoundPacketBuilder(DX_WORLD_SETTINGS), spawn, time, timeScale)
}

fun clientBoundHeartbeat(): Packet {
  return heartbeatPacketBuilder(clientBoundPacketBuilder(DX_HEARTBEAT))
}

fun clientBoundContainerUpdate(ownedContainer: OwnedContainer): Packet = containerUpdateBuilder(clientBoundPacketBuilder(DX_CONTAINER_UPDATE), ownedContainer)

fun clientBoundInterfaceUpdate(interfaceId: String, updateType: InterfaceUpdate.UpdateType): Packet =
  clientBoundPacketBuilder(CB_INTERFACE_UPDATE).setInterfaceUpdate(
    interfaceUpdate {
      this.interfaceId = interfaceId
      this.updateType = updateType
    }
  ).build()

// ////////////
//   DUAL   //
// ////////////

/**
 * Helper method to send either a server bound or client bound packet depending on which this instance currently is.
 *
 * @param ifIsServer The packet to send if we are the server
 * @param ifIsClient The packet to send if we are a server client
 */
fun sendDuplexPacket(ifIsServer: () -> Packet, ifIsClient: ServerClient.() -> Packet) {
  if (Main.isServer) {
    Main.inst().packetBroadcaster.broadcast(ifIsServer())
  } else if (Main.isServerClient) {
    val client = ClientMain.inst().serverClient ?: error("Server client null after check")
    client.ctx.writeAndFlushPacket(client.ifIsClient())
  }
}

/**
 * Helper method to send either a server bound or client bound packet depending on which this instance currently is.
 *
 * @param ifIsServer The packet to send if we are the server, and the chunk to send it to
 * @param ifIsClient The packet to send if we are a server client
 */
fun sendDuplexPacketInView(ifIsServer: () -> Pair<Packet?, ChunkCompactLoc>, ifIsClient: ServerClient.() -> Packet?) {
  if (Main.isServer) {
    val (maybePacket, chunkLoc) = ifIsServer()
    val packet = maybePacket ?: return
    Main.inst().packetBroadcaster.broadcastToInViewChunk(packet, chunkLoc.decompactLocX(), chunkLoc.decompactLocY())
  } else if (Main.isServerClient) {
    val client = ClientMain.inst().serverClient ?: error("Server client null after check")
    val packet = client.ifIsClient() ?: return
    client.ctx.writeAndFlushPacket(packet)
  }
}

// /////////////////////
//   DUAL Builders   //
// /////////////////////

private fun worldSettingsPacketBuilder(packet: Packet.Builder, spawn: Long?, time: Float?, timeScale: Float?): Packet {
  return packet.setWorldSettings(
    WorldSettings.newBuilder()?.also {
      if (spawn != null) {
        it.spawn = spawn.toVector2i()
      }
      if (time != null) {
        it.time = time
      }
      if (timeScale != null) {
        it.timeScale = timeScale
      }
    }
  ).build()
}

private fun heartbeatPacketBuilder(packet: Packet.Builder): Packet {
  return packet.setHeartbeat(Heartbeat.newBuilder().setKeepAliveId(Instant.now().epochSecond.toString()).build()).build()
}

fun containerUpdateBuilder(packet: Packet.Builder, ownedContainer: OwnedContainer): Packet =
  packet.setContainerUpdate(containerUpdate { worldContainer = ownedContainer.asProto() }).build()
