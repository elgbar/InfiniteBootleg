package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import java.util.UUID
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.ChunkRequest
import no.elg.infiniteBootleg.protobuf.Packets.Disconnect
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Direction.CLIENT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Direction.SERVER
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_LOGIN_STATUS
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_START_GAME
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_UPDATE_CHUNK
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BLOCK_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_DISCONNECT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_SECRET_EXCHANGE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CHUNK_REQUEST
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_LOGIN
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.protobuf.Packets.StartGame
import no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock
import no.elg.infiniteBootleg.protobuf.Packets.UpdateChunk
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.subgrid.enitites.Player


internal fun ChannelHandlerContext.fatal(msg: String) {
  if (Settings.client) {
    ConnectingScreen.info = msg
  } else {
    this.writeAndFlush(disconnectClientPacket(msg))
  }
  close()
  Main.inst().consoleLogger.error("ERR", msg)
}

/**
 * @author Elg
 */

fun Client.serverBoundPacket(type: Type): Packets.Packet.Builder {
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


fun loginPacket(name: String, uuid: UUID): Packets.Packet {
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

fun Client.serverBoundBlockUpdate(worldX: Int, worldY: Int, block: Block?): Packets.Packet {
  return serverBoundPacket(DX_BLOCK_UPDATE).setBlockUpdate(
    UpdateBlock.newBuilder()
      .setPos(Vector2i.newBuilder().setX(worldX).setY(worldY))
      .also {
        if (block != null) {
          it.setBlock(block.save())
        }
      }
  ).build()
}

fun Client.clientSecretResponse(connectionCredentials: ConnectionCredentials): Packets.Packet {
  return serverBoundPacket(DX_SECRET_EXCHANGE).setSecretExchange(
    SecretExchange.newBuilder()
      .setSecret(connectionCredentials.secret)
      .setEntityUUID(connectionCredentials.entityUUID.toString())
  ).build()
}


fun Client.chunkRequestPacket(chunkLocation: Location): Packets.Packet {
  return serverBoundPacket(SB_CHUNK_REQUEST).setChunkRequest(ChunkRequest.newBuilder().setChunkLocation(chunkLocation.toVector2i())).build()
}

//////////////////
// client bound //
//////////////////


fun clientBoundBlockUpdate(loc: Location, block: Block): Packets.Packet {
  return clientBoundPacket(DX_BLOCK_UPDATE).setBlockUpdate(
    UpdateBlock.newBuilder()
      .setBlock(block.save())
      .setPos(loc.toVector2i())
  ).build()
}

fun serverLoginStatusPacket(status: ServerLoginStatus.ServerStatus): Packets.Packet {
  return clientBoundPacket(CB_LOGIN_STATUS).setServerLoginStatus(ServerLoginStatus.newBuilder().setStatus(status)).build()
}

fun startGamePacket(player: Player): Packets.Packet {
  return clientBoundPacket(CB_START_GAME).setStartGame(
    StartGame.newBuilder()
      .setWorld(player.world.toProtobuf())
      .setControlling(player.save())
  ).build()
}

fun updateChunkPacket(chunk: Chunk): Packets.Packet {
  return clientBoundPacket(CB_UPDATE_CHUNK).setUpdateChunk(UpdateChunk.newBuilder().setChunk(chunk.save())).build()
}

fun disconnectClientPacket(reason: String?) {
  return clientBoundPacket(DX_DISCONNECT).let {
    if (!reason.isNullOrBlank()) {
      it.setDisconnect(Disconnect.newBuilder().setReason(reason))
    }
    it.build()
  }
}

fun serverSecretExchange(connectionCredentials: ConnectionCredentials): Packets.Packet {
  return clientBoundPacket(DX_SECRET_EXCHANGE).setSecretExchange(
    SecretExchange.newBuilder()
      .setSecret(connectionCredentials.secret)
      .setEntityUUID(connectionCredentials.entityUUID.toString())
  ).build()
}

