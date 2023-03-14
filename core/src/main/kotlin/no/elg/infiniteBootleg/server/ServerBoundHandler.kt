package no.elg.infiniteBootleg.server

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.ServerMain
import no.elg.infiniteBootleg.protobuf.Packets
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Elg
 */
class ServerBoundHandler : SimpleChannelInboundHandler<Packets.Packet>() {
  override fun channelRead0(ctx: ChannelHandlerContext, packet: Packets.Packet) {
    Main.logger().log("Server bound packet ${packet.type}")
    packetsReceived++
    if (packet.direction == Packets.Packet.Direction.CLIENT || packet.type.name.startsWith("CB_")) {
      ctx.fatal("Server got a client packet ${packet.type} direction ${packet.direction}")
      return
    } else if (packet.type != Packets.Packet.Type.SB_LOGIN) {
      val expectedSecret = clients[ctx.channel()]
      if (expectedSecret == null) {
        ctx.fatal("Unknown client")
        return
      }
      if (expectedSecret.secret != packet.secret) {
        ctx.fatal("Invalid secret given")
        return
      }
    }
    Main.inst().scheduler.executeSync { handleServerBoundPackets(ctx, packet) }
  }

  override fun channelActive(ctx: ChannelHandlerContext) {
    channels.add(ctx.channel())
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    channels.remove(ctx.channel())
    val client = clients.remove(ctx.channel())
    val playerId = client?.entityUUID ?: "<Unknown>"
    Main.logger().debug(TAG, "client inactive (player $playerId) (curr active ${clients.size} clients, ${channels.size} channels)")
    if (client != null) {
      val task = client.heartbeatTask
      task?.cancel(false)
      ServerMain.inst().serverWorld.disconnectPlayer(client.entityUUID, false)
    }
  }

  companion object {
    val channels: ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
    val clients: MutableMap<Channel, SharedInformation> = ConcurrentHashMap()
    const val TAG = "SERVER"
    var packetsReceived: Long = 0
  }
}