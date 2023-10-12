package no.elg.infiniteBootleg.server

import com.badlogic.ashley.core.Entity
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.group.ChannelMatcher
import io.netty.channel.group.ChannelMatchers
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.main.ServerMain
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.protobuf.Packets.Disconnect
import no.elg.infiniteBootleg.protobuf.Packets.EntityRequest
import no.elg.infiniteBootleg.protobuf.Packets.Heartbeat
import no.elg.infiniteBootleg.protobuf.Packets.MoveEntity
import no.elg.infiniteBootleg.protobuf.Packets.Packet
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Direction.CLIENT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Direction.SERVER
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_DESPAWN_ENTITY
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
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CHUNK_REQUEST
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_ENTITY_REQUEST
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_LOGIN
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.protobuf.Packets.SpawnEntity
import no.elg.infiniteBootleg.protobuf.Packets.StartGame
import no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock
import no.elg.infiniteBootleg.protobuf.Packets.UpdateChunk
import no.elg.infiniteBootleg.protobuf.Packets.WorldSettings
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.server.ServerBoundHandler.Companion.channels
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.toComponentsString
import no.elg.infiniteBootleg.util.toVector2i
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.Material.AIR
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.world.ecs.save
import java.time.Instant
import java.util.UUID

// //////////////////
// util functions //
// //////////////////

internal fun ChannelHandlerContext.fatal(msg: String) {
  if (Settings.client) {
    Main.inst().scheduler.executeSync {
      ConnectingScreen.info = msg
      ClientMain.inst().screen = ConnectingScreen
      val serverClient = ClientMain.inst().serverClient
      if (serverClient?.sharedInformation != null) {
        this.writeAndFlush(serverClient.serverBoundClientDisconnectPacket(msg))
      }
      Main.inst().scheduler.scheduleSync(50L) {
        close()
      }
    }
  } else {
    this.writeAndFlush(clientBoundDisconnectPlayerPacket(msg))
  }
  Main.logger().error("IO FATAL", msg)
}

/**
 * Broadcast to all other channels than [this]
 */
fun broadcast(packet: Packet, filter: ChannelMatcher = ChannelMatchers.all()) {
  require(Main.isServer) { "This broadcasting methods can only be used by servers" }
  channels.writeAndFlush(packet, filter)
}

/**
 * Broadcast a packet to players which have the given [worldPosition] location loaded.
 *
 * Can only be used by a server instance
 */
fun broadcastToInView(packet: Packet, worldX: WorldCoord, worldY: WorldCoord, filter: ((Channel) -> Boolean)? = null) {
  require(Main.isServer) { "This broadcasting methods can only be used by servers" }
  val world = ServerMain.inst().serverWorld
  val renderer = world.render
  val chunkX = worldX.worldToChunk()
  val chunkY = worldY.worldToChunk()
  broadcast(packet) { channel ->
    val sharedInfo = ServerBoundHandler.clients[channel] ?: return@broadcast false
    val viewing = renderer.getClient(sharedInfo.entityUUID) ?: return@broadcast false
    return@broadcast viewing.isInView(chunkX, chunkY) && filter?.invoke(channel) ?: true
  }
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

// ////////////////
// server bound //
// ////////////////

fun serverBoundLoginPacket(name: String, uuid: UUID): Packet {
  return Packet.newBuilder()
    .setDirection(SERVER)
    .setType(SB_LOGIN)
    .setLogin(
      Packets.Login.newBuilder()
        .setUsername(name)
        .setUuid(uuid.toString())
        .setVersion(Util.getVersion())
    ).build()
}

fun ServerClient.serverBoundBlockUpdate(worldX: WorldCoord, worldY: WorldCoord, block: Block?): Packet {
  return serverBoundPacketBuilder(DX_BLOCK_UPDATE).setUpdateBlock(
    UpdateBlock.newBuilder()
      .setPos(Vector2i.newBuilder().setX(worldX).setY(worldY))
      .setBlock(block?.save()?.build() ?: PROTO_AIR_BLOCK)
  ).build()
}

fun ServerClient.serverBoundClientSecretResponse(sharedInformation: SharedInformation): Packet {
  return serverBoundPacketBuilder(DX_SECRET_EXCHANGE).setSecretExchange(
    SecretExchange.newBuilder()
      .setSecret(sharedInformation.secret)
      .setEntityUUID(sharedInformation.entityUUID)
  ).build()
}

fun ServerClient.serverBoundChunkRequestPacket(x: Int, y: Int): Packet {
  return serverBoundChunkRequestPacket(Vector2i.newBuilder().setX(x).setY(y).build())
}

fun ServerClient.serverBoundChunkRequestPacket(chunkLocation: Vector2i): Packet {
  return serverBoundPacketBuilder(SB_CHUNK_REQUEST).setChunkRequest(Packets.ChunkRequest.newBuilder().setChunkLocation(chunkLocation)).build()
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
  return serverBoundPacketBuilder(DX_MOVE_ENTITY).setMoveEntity(
    MoveEntity.newBuilder()
      .setUuid(entity.id)
      .setPosition(entity.positionComponent.toProtoVector2f())
      .setVelocity(entity.velocityComponent.toVector2f())
//      .setLookAngleDeg(entity.lookDeg)
  ).build()
}

fun ServerClient.serverBoundEntityRequest(uuid: String): Packet {
  return serverBoundPacketBuilder(SB_ENTITY_REQUEST).setEntityRequest(
    EntityRequest.newBuilder()
      .setUuid(uuid)
  ).build()
}

fun ServerClient.serverBoundWorldSettings(spawn: Long?, time: Float?, timeScale: Float?): Packet {
  return worldSettingsPacketBuilder(serverBoundPacketBuilder(DX_WORLD_SETTINGS), spawn, time, timeScale)
}

fun ServerClient.serverBoundHeartbeat(): Packet {
  return heartbeatPacketBuilder(serverBoundPacketBuilder(DX_HEARTBEAT))
}

// ////////////////
// client bound //
// ////////////////

private val PROTO_AIR_BLOCK = Block.save(AIR).build()

fun clientBoundBlockUpdate(worldX: WorldCoord, worldY: WorldCoord, block: Block?): Packet {
  return clientBoundPacketBuilder(DX_BLOCK_UPDATE).setUpdateBlock(
    UpdateBlock.newBuilder()
      .setBlock(block?.save()?.build() ?: PROTO_AIR_BLOCK)
      .setPos(Vector2i.newBuilder().setX(worldX).setY(worldY))
  ).build()
}

fun clientBoundSpawnEntity(entity: Entity): Packet {
  if (entity.authoritativeOnly) {
    throw IllegalStateException("Cannot send entity with the tag authoritative only to clients ${entity.toComponentsString()}")
  }
  return clientBoundPacketBuilder(CB_SPAWN_ENTITY).setSpawnEntity(
    SpawnEntity.newBuilder()
      .setEntity(entity.save())
      .setUuid(entity.id)
  ).build()
}

fun clientBoundDespawnEntity(uuid: String, reason: DespawnReason): Packet {
  return clientBoundPacketBuilder(CB_DESPAWN_ENTITY).setDespawnEntity(
    DespawnEntity.newBuilder()
      .setUuid(uuid)
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
      .setControlling(player.save())
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
      .setEntityUUID(sharedInformation.entityUUID)
  ).build()
}

fun clientBoundWorldSettings(spawn: Long?, time: Float?, timeScale: Float?): Packet {
  return worldSettingsPacketBuilder(clientBoundPacketBuilder(DX_WORLD_SETTINGS), spawn, time, timeScale)
}

fun clientBoundHeartbeat(): Packet {
  return heartbeatPacketBuilder(clientBoundPacketBuilder(DX_HEARTBEAT))
}

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
    broadcast(ifIsServer())
  } else if (Main.isServerClient) {
    val client = ClientMain.inst().serverClient ?: error("Server client null after check")
    client.ctx.writeAndFlush(client.ifIsClient())
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
