package no.elg.infiniteBootleg.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor
import no.elg.infiniteBootleg.main.ServerMain
import no.elg.infiniteBootleg.protobuf.Packets
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class ServerBoundHandler : SimpleChannelInboundHandler<Packets.Packet>() {

  private val ctxToWrapper = ConcurrentHashMap<ChannelHandlerContext, ChannelHandlerContextWrapper>()

  override fun channelRead0(ctx: ChannelHandlerContext, packet: Packets.Packet) {
    packetsReceived++
    val wrappedCtx = ctxToWrapper.computeIfAbsent(ctx) { ChannelHandlerContextWrapper("server->client", ctx) }
    if (packet.direction == Packets.Packet.Direction.CLIENT || packet.type.name.startsWith("CB_")) {
      wrappedCtx.fatal("Server got a client packet ${packet.type} direction ${packet.direction}")
      return
    }
    if (packet.type != Packets.Packet.Type.SB_LOGIN) {
      val expectedSecret = clients[ctx.channel()]
      if (expectedSecret == null) {
        wrappedCtx.fatal("Unknown client")
        return
      }
      if (expectedSecret.secret != packet.secret) {
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
    val playerId = client?.entityUUID ?: "<Unknown>"
    logger.debug { "client inactive (player $playerId) (curr active ${clients.size} clients, ${channels.size} channels)" }
    if (client != null) {
      val task = client.heartbeatTask
      task?.cancel(false)
      ServerMain.inst().serverWorld.disconnectPlayer(client.entityUUID, false)
    }
  }

  companion object {
    val channels: ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
    val clients: MutableMap<Channel, SharedInformation> = ConcurrentHashMap()
    var packetsReceived: Long = 0
  }
}
