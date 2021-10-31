package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import java.util.UUID
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.Disconnect
import no.elg.infiniteBootleg.protobuf.Packets.MoveEntity
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Direction.CLIENT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Direction.SERVER
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_LOGIN_STATUS
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_START_GAME
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_UPDATE_CHUNK
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BLOCK_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_DISCONNECT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_MOVE_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_SECRET_EXCHANGE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_LOGIN
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
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
    if (serverClient != null) {
      this.writeAndFlush(serverClient.serverBoundClientDisconnectPacket(msg))
      ClientMain.inst().serverClient = null
    }
  } else {
    this.writeAndFlush(clientBoundDisconnectPlayerPacket(msg))
  }
  Main.logger().error("IO FATAL", msg)
  Main.inst().scheduler.scheduleSync({ close() }, 50L)
}


/**
 * Broadcast to all other channels than [this]
 */
fun ChannelHandlerContext?.broadcast(packet: Packets.Packet) {
  val channel = this?.channel()
  for ((client, _) in ServerBoundHandler.clients) {
    if (client == channel) continue
    client.writeAndFlush(packet)
  }
}

/**
 * @author Elg
 */
fun ServerClient.serverBoundPacket(type: Type): Packets.Packet.Builder {
  return Packets.Packet.newBuilder()
    .setDirection(SERVER)
    .setSecret(credentials.secret)
    .setType(type)
}

fun clientBoundPacket(type: Type): Packets.Packet.Builder {
  return Packets.Packet.newBuilder()
    .setDirection(CLIENT)
    .setType(type)
}


//////////////////
// server bound //
//////////////////


fun serverBoundLoginPacket(name: String, uuid: UUID): Packets.Packet {
  return Packets.Packet.newBuilder()
    .setDirection(SERVER)
    .setType(SB_LOGIN)
    .setLogin(
      Packets.Login.newBuilder()
        .setUsername(name)
        .setUuid(uuid.toString())
        .setVersion(Util.getVersion())
    ).build()
}

fun ServerClient.serverBoundBlockUpdate(worldX: Int, worldY: Int, block: Block?): Packets.Packet {
  return serverBoundPacket(DX_BLOCK_UPDATE).setUpdateBlock(
    UpdateBlock.newBuilder()
      .setPos(Vector2i.newBuilder().setX(worldX).setY(worldY))
      .also {
        if (block != null) {
          it.setBlock(block.save())
        }
      }
  ).build()
}

fun ServerClient.serverBoundClientSecretResponse(connectionCredentials: ConnectionCredentials): Packets.Packet {
  return serverBoundPacket(DX_SECRET_EXCHANGE).setSecretExchange(
    SecretExchange.newBuilder()
      .setSecret(connectionCredentials.secret)
      .setEntityUUID(connectionCredentials.entityUUID.toString())
  ).build()
}


//fun Client.chunkRequestPacket(chunkLocation: Location): Packets.Packet {
//  return serverBoundPacket(SB_CHUNK_REQUEST).setChunkRequest(ChunkRequest.newBuilder().setChunkLocation(chunkLocation.toVector2i())).build()
//}


fun ServerClient.serverBoundClientDisconnectPacket(reason: String?): Packets.Packet {
  return serverBoundPacket(DX_DISCONNECT).let {
    if (!reason.isNullOrBlank()) {
      it.setDisconnect(Disconnect.newBuilder().setReason(reason))
    }
    it.build()
  }
}

fun ServerClient.serverBoundMoveEntityPacket(entity: Entity): Packets.Packet {
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


private val AIR_BLOCK = Block.save(AIR)

fun clientBoundBlockUpdate(worldX: Int, worldY: Int, block: Block?): Packets.Packet {
  return clientBoundPacket(DX_BLOCK_UPDATE).setUpdateBlock(
    UpdateBlock.newBuilder()
      .setBlock(if (block != null) block.save() else AIR_BLOCK)
      .setPos(Vector2i.newBuilder().setX(worldX).setY(worldY))
  ).build()
}

fun clientBoundMoveEntity(entity: Entity): Packets.Packet {
  return clientBoundPacket(DX_MOVE_ENTITY).setMoveEntity(
    MoveEntity.newBuilder()
      .setUuid(entity.uuid.toString()) //
      .setPosition(entity.position.toVector2f()) //
      .setVelocity(entity.velocity.toVector2f()) //
  ).build()
}

fun clientBoundLoginStatusPacket(status: ServerLoginStatus.ServerStatus): Packets.Packet {
  return clientBoundPacket(CB_LOGIN_STATUS).setServerLoginStatus(ServerLoginStatus.newBuilder().setStatus(status)).build()
}

fun clientBoundStartGamePacket(player: Player): Packets.Packet {
  return clientBoundPacket(CB_START_GAME).setStartGame(
    StartGame.newBuilder()
      .setWorld(player.world.toProtobuf())
      .setControlling(player.save())
  ).build()
}

fun clientBoundUpdateChunkPacket(chunk: Chunk): Packets.Packet {
  return clientBoundPacket(CB_UPDATE_CHUNK).setUpdateChunk(UpdateChunk.newBuilder().setChunk(chunk.save())).build()
}

fun clientBoundDisconnectPlayerPacket(reason: String?): Packets.Packet {
  return clientBoundPacket(DX_DISCONNECT).let {
    if (!reason.isNullOrBlank()) {
      it.setDisconnect(Disconnect.newBuilder().setReason(reason))
    }
    it.build()
  }
}

fun clientBoundSecretExchange(connectionCredentials: ConnectionCredentials): Packets.Packet {
  return clientBoundPacket(DX_SECRET_EXCHANGE).setSecretExchange(
    SecretExchange.newBuilder()
      .setSecret(connectionCredentials.secret)
      .setEntityUUID(connectionCredentials.entityUUID.toString())
  ).build()
}

