package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import java.util.UUID
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.ChunkRequest
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BLOCK_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_DISCONNECT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_HEARTBEAT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_MOVE_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CHUNK_REQUEST
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CLIENT_WORLD_LOADED
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_LOGIN
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.UNRECOGNIZED
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.subgrid.enitites.Player


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
    SB_CLIENT_WORLD_LOADED -> handleLoginStatusPacket(ctx)
    SB_CHUNK_REQUEST -> {
      if (packet.hasChunkRequest()) {
        handleChunkRequest(ctx, packet.chunkRequest)
      }
    }

    DX_HEARTBEAT -> TODO()
    DX_MOVE_ENTITY -> TODO()
    DX_BLOCK_UPDATE -> TODO()
    DX_DISCONNECT -> ctx.close()

    UNRECOGNIZED -> ctx.fatal("Unknown packet type received")
    else -> ctx.fatal("Cannot handle packet of type " + packet.type)
  }
}

fun handleChunkRequest(ctx: ChannelHandlerContext, chunkRequest: ChunkRequest) {
  val chunkLoc = Location.fromVector2i(chunkRequest.chunkLocation)
  val chunk = Main.inst().world.getChunk(chunkLoc) ?: return // if no chunk, don't send a chunk update
  ctx.writeAndFlush(updateChunkPacket(chunk))
}

private fun handleLoginStatusPacket(ctx: ChannelHandlerContext) {
  val world = Main.inst().world
  for (chunk in world.loadedChunks) {
    ctx.writeAndFlush(updateChunkPacket(chunk))
  }
  ctx.writeAndFlush(serverLoginStatusPacket(ServerLoginStatus.ServerStatus.LOGIN_SUCCESS))
}

private fun handleLoginPacket(ctx: ChannelHandlerContext, login: Packets.Login) {
  val version = Util.getVersion()
  if (login.version != version) {
    ctx.fatal("Version mismatch! Client: '${login.version}' Server: '$version'")
    return
  }

  val world = Main.inst().world
  val uuid = try {
    UUID.fromString(login.uuid)
  } catch (e: IllegalArgumentException) {
    ctx.fatal("Failed to decode login UUID ${login.uuid}")
    return
  }

  if (world.hasLivingEntity(uuid)) {
    ctx.writeAndFlush(serverLoginStatusPacket(ServerLoginStatus.ServerStatus.ALREADY_LOGGED_IN))
    ctx.close()
    return
  }
  ctx.writeAndFlush(serverLoginStatusPacket(ServerLoginStatus.ServerStatus.PROCEED_LOGIN))

  //TODO actually save players
  val player = Player(world, world.spawn.x.toFloat(), world.spawn.y.toFloat())
  if (player.isInvalid) {
    ctx.fatal("Failed to spawn player server side")
    return
  }
  player.name = login.username
  world.addEntity(player)
  ctx.writeAndFlush(startGamePacket(player))
}
