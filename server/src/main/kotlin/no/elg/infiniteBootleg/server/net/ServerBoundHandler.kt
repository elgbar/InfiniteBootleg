package no.elg.infiniteBootleg.server.net

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor
import no.elg.infiniteBootleg.console.serverSideClientBoundMarker
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.server.ChannelHandlerContextWrapper
import no.elg.infiniteBootleg.server.ServerMain
import no.elg.infiniteBootleg.server.SharedInformation
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class ServerBoundHandler : SimpleChannelInboundHandler<Packets.Packet>() {

  private val ctxToWrapper = ConcurrentHashMap<ChannelHandlerContext, ChannelHandlerContextWrapper>()

  override fun channelRead0(ctx: ChannelHandlerContext, packet: Packets.Packet) {
    packetsReceived++
    val wrappedCtx = ctxToWrapper.computeIfAbsent(ctx) {
      ChannelHandlerContextWrapper(
        serverSideClientBoundMarker,
        ctx
      )
    }
    if (packet.direction == Packets.Packet.Direction.CLIENT || packet.type.name.startsWith("CB_")) {
      wrappedCtx.fatal("Server got a client packet ${packet.type} direction ${packet.direction}")
      return
    }
    if (packet.type != Packets.Packet.Type.SB_LOGIN) {
      val sharedInformation = clients[ctx.channel()]
      if (sharedInformation == null) {
        wrappedCtx.fatal("Unknown client")
        return
      }
      if (sharedInformation.secret != packet.secret) {
        wrappedCtx.fatal("Invalid secret given")
        return
      }
    }
    handleServerBoundPackets(wrappedCtx, packet)
  }

  override fun channelActive(ctx: ChannelHandlerContext) {
    channels.add(ctx.channel())
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    channels.remove(ctx.channel())
    ctxToWrapper.remove(ctx)
    val client = clients.remove(ctx.channel())
    val playerId = client?.entityId ?: "<Unknown>"
    logger.debug { "client inactive (player $playerId) (curr active ${clients.size} clients, ${channels.size} channels)" }
    if (client != null) {
      client.heartbeatTask?.cancel(false)
      ServerMain.Companion.inst().serverWorld.disconnectPlayer(client.entityId, false)
    }
  }

  companion object {
    val channels: ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
    val clients: MutableMap<Channel, SharedInformation> = ConcurrentHashMap()
    var packetsReceived: Long = 0
  }
}
