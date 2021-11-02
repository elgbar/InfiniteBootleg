package no.elg.infiniteBootleg.server

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import java.util.UUID
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity
import no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
import no.elg.infiniteBootleg.protobuf.Packets.Disconnect
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
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_MOVE_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_SECRET_EXCHANGE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_LOGIN
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.protobuf.Packets.SpawnEntity
import no.elg.infiniteBootleg.protobuf.Packets.StartGame
import no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock
import no.elg.infiniteBootleg.protobuf.Packets.UpdateChunk
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.util.toVector2f
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Material.AIR
import no.elg.infiniteBootleg.world.subgrid.Entity
import no.elg.infiniteBootleg.world.subgrid.enitites.Player


////////////////////
// util functions //
////////////////////

internal fun ChannelHandlerContext.fatal(msg: String) {
  if (Settings.client) {
    ConnectingScreen.info = msg
    val serverClient = ClientMain.inst().serverClient
    if (serverClient?.credentials != null) {
      this.writeAndFlush(serverClient.serverBoundClientDisconnectPacket(msg))
    }
  } else {
    this.writeAndFlush(clientBoundDisconnectPlayerPacket(msg))
  }
  Main.logger().error("IO FATAL", msg)
  Main.inst().scheduler.scheduleSync({
    close()
    ClientMain.inst().serverClient = null
  }, 50L)
}


/**
 * Broadcast to all other channels than [this]
 */
fun ChannelHandlerContext?.broadcast(packet: Packet, filter: ((Channel, ConnectionCredentials) -> Boolean)? = null) {
  val thisChannel = this?.channel()
  for ((channel, creds) in ServerBoundHandler.clients) {
    if (channel == thisChannel || (filter != null && !filter(channel, creds))) {
      continue
    }
    channel.writeAndFlush(packet)
  }
}

/**
 * @author Elg
 */
fun ServerClient.serverBoundPacket(type: Type): Packet.Builder {
  return Packet.newBuilder()
    .setDirection(SERVER)
    .setSecret(credentials!!.secret) //FIXME
    .setType(type)
}

fun clientBoundPacket(type: Type): Packet.Builder {
  return Packet.newBuilder()
    .setDirection(CLIENT)
    .setType(type)
}


//////////////////
// server bound //
//////////////////


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

fun ServerClient.serverBoundBlockUpdate(worldX: Int, worldY: Int, block: Block?): Packet {
  return serverBoundPacket(DX_BLOCK_UPDATE).setUpdateBlock(
    UpdateBlock.newBuilder()
      .setPos(Vector2i.newBuilder().setX(worldX).setY(worldY))
      .setBlock(block?.save()?.build() ?: PROTO_AIR_BLOCK)
  ).build()
}

fun ServerClient.serverBoundClientSecretResponse(connectionCredentials: ConnectionCredentials): Packet {
  return serverBoundPacket(DX_SECRET_EXCHANGE).setSecretExchange(
    SecretExchange.newBuilder()
      .setSecret(connectionCredentials.secret)
      .setEntityUUID(connectionCredentials.entityUUID.toString())
  ).build()
}


//fun Client.chunkRequestPacket(chunkLocation: Location): Packets.Packet {
//  return serverBoundPacket(SB_CHUNK_REQUEST).setChunkRequest(ChunkRequest.newBuilder().setChunkLocation(chunkLocation.toVector2i())).build()
//}


fun ServerClient.serverBoundClientDisconnectPacket(reason: String?): Packet {
  return serverBoundPacket(DX_DISCONNECT).let {
    if (!reason.isNullOrBlank()) {
      it.setDisconnect(Disconnect.newBuilder().setReason(reason))
    }
    it.build()
  }
}

fun ServerClient.serverBoundMoveEntityPacket(entity: Entity): Packet {
  return serverBoundPacket(DX_MOVE_ENTITY).setMoveEntity(
    MoveEntity.newBuilder()
      .setUuid(entity.uuid.toString()) //
      .setPosition(entity.position.toVector2f()) //
      .setVelocity(entity.velocity.toVector2f()) //
  ).build()
}

//////////////////
// client bound //
//////////////////


private val PROTO_AIR_BLOCK = Block.save(AIR).build()

fun clientBoundBlockUpdate(worldX: Int, worldY: Int, block: Block?): Packet {
  return clientBoundPacket(DX_BLOCK_UPDATE).setUpdateBlock(
    UpdateBlock.newBuilder()
      .setBlock(block?.save()?.build() ?: PROTO_AIR_BLOCK)
      .setPos(Vector2i.newBuilder().setX(worldX).setY(worldY))
  ).build()
}

fun clientBoundMoveEntity(entity: Entity): Packet {
  return clientBoundPacket(DX_MOVE_ENTITY).setMoveEntity(
    MoveEntity.newBuilder()
      .setUuid(entity.uuid.toString()) //
      .setPosition(entity.position.toVector2f()) //
      .setVelocity(entity.velocity.toVector2f()) //
  ).build()
}

fun clientBoundSpawnEntity(entity: Entity): Packet {
  return clientBoundPacket(CB_SPAWN_ENTITY).setSpawnEntity(
    SpawnEntity.newBuilder()
      .setEntity(entity.save())
  ).build()
}

fun clientBoundDespawnEntity(entity: Entity, reason: DespawnReason): Packet {
  return clientBoundPacket(CB_DESPAWN_ENTITY).setDespawnEntity(
    DespawnEntity.newBuilder()
      .setUuid(entity.uuid.toString())
      .setDespawnReason(reason)
  ).build()
}

fun clientBoundLoginStatusPacket(status: ServerLoginStatus.ServerStatus): Packet {
  return clientBoundPacket(CB_LOGIN_STATUS).setServerLoginStatus(ServerLoginStatus.newBuilder().setStatus(status)).build()
}

fun clientBoundStartGamePacket(player: Player): Packet {
  return clientBoundPacket(CB_START_GAME).setStartGame(
    StartGame.newBuilder()
      .setWorld(player.world.toProtobuf())
      .setControlling(player.save())
  ).build()
}

fun clientBoundUpdateChunkPacket(chunk: Chunk): Packet {
  return clientBoundPacket(CB_UPDATE_CHUNK).setUpdateChunk(UpdateChunk.newBuilder().setChunk(chunk.saveBlocksOnly())).build()
}

fun clientBoundDisconnectPlayerPacket(reason: String?): Packet {
  return clientBoundPacket(DX_DISCONNECT).let {
    if (!reason.isNullOrBlank()) {
      it.setDisconnect(Disconnect.newBuilder().setReason(reason))
    }
    it.build()
  }
}

fun clientBoundSecretExchange(connectionCredentials: ConnectionCredentials): Packet {
  return clientBoundPacket(DX_SECRET_EXCHANGE).setSecretExchange(
    SecretExchange.newBuilder()
      .setSecret(connectionCredentials.secret)
      .setEntityUUID(connectionCredentials.entityUUID.toString())
  ).build()
}

