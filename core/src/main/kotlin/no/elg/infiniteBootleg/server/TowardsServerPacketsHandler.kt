package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import java.security.SecureRandom
import java.util.UUID
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.ChunkRequest
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BLOCK_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_DISCONNECT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_HEARTBEAT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_MOVE_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_SECRET_EXCHANGE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CHUNK_REQUEST
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CLIENT_WORLD_LOADED
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_LOGIN
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.UNRECOGNIZED
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.loader.WorldLoader

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
    SB_CLIENT_WORLD_LOADED -> handleLoginStatusPacket(ctx)
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
    DX_HEARTBEAT -> TODO()
    DX_MOVE_ENTITY -> TODO()
    DX_BLOCK_UPDATE -> {
      if (packet.hasBlockUpdate()) {
        handleBlockUpdate(packet.blockUpdate)
      }
    }
    DX_DISCONNECT -> ctx.close()

    UNRECOGNIZED -> ctx.fatal("Unknown packet type received")
    else -> ctx.fatal("Cannot handle packet of type " + packet.type)
  }
}

fun handleSecretExchange(ctx: ChannelHandlerContext, secretExchange: SecretExchange) {
  val cc = ServerBoundHandler.clients[ctx.channel()]
  if (cc == null) {
    ctx.fatal("No secret with this channel, send login request first")
    return
  }

  try {
    val uuid = UUID.fromString(secretExchange.entityUUID)
    if (uuid != cc.entityUUID || secretExchange.secret != cc.secret) {
      ctx.fatal("Wrong secret returned!")
      return
    }
    val player = Main.inst().world.getPlayer(uuid)
    if (player == null) {
      ctx.fatal("Failed secret response")
    } else {
      ctx.writeAndFlush(startGamePacket(player))
    }
  } catch (e: Exception) {
    ctx.fatal("Failed to parse entity in secret")
    return
  }
}

fun handleBlockUpdate(blockUpdate: UpdateBlock) {
  val worldX = blockUpdate.pos.x
  val worldY = blockUpdate.pos.y
  val protoBlock = if (blockUpdate.hasBlock()) blockUpdate.block else null
  Main.inst().world.setBlock(worldX, worldY, protoBlock)
}

fun handleChunkRequest(ctx: ChannelHandlerContext, chunkRequest: ChunkRequest) {
  val chunkLoc = Location.fromVector2i(chunkRequest.chunkLocation)
  val chunk = Main.inst().world.getChunk(chunkLoc) ?: return // if no chunk, don't send a chunk update
  val allowedUnload = chunk.isAllowingUnloading
  chunk.setAllowUnload(false)
  ctx.writeAndFlush(updateChunkPacket(chunk))
  chunk.setAllowUnload(allowedUnload)
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

  if (world.hasPlayer(uuid)) {
    ctx.writeAndFlush(serverLoginStatusPacket(ServerLoginStatus.ServerStatus.ALREADY_LOGGED_IN))
    ctx.close()
    return
  }
  ctx.writeAndFlush(serverLoginStatusPacket(ServerLoginStatus.ServerStatus.PROCEED_LOGIN))

  val player = WorldLoader.getServerPlayer(world, uuid)
  if (player.isInvalid) {
    ctx.fatal("Failed to spawn player server side")
    return
  }
  player.name = login.username
  WorldLoader.saveServerPlayer(player)

  val secret = UUID.nameUUIDFromBytes(ByteArray(128).also { secureRandom.nextBytes(it) })

  val connectionCredentials = ConnectionCredentials(player.uuid, secret.toString())
  ServerBoundHandler.clients[ctx.channel()] = connectionCredentials

  ctx.writeAndFlush(serverSecretExchange(connectionCredentials))
}
