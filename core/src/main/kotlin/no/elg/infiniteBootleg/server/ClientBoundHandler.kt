package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.screens.ConnectingScreen.info

/**
 * @author Elg
 */
class ClientBoundHandler(private val client: ServerClient) : SimpleChannelInboundHandler<Packets.Packet>() {
  override fun channelActive(ctx: ChannelHandlerContext) {
    client.ctx = ctx
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    Main.inst().scheduler.executeSync {
      val serverClient = ClientMain.inst().serverClient
      if (serverClient != null) {
        val sharedInformation = serverClient.sharedInformation
        if (sharedInformation != null) {
          val task = sharedInformation.heartbeatTask
          task?.cancel(false)
        }
      }
      ClientMain.inst().screen = ConnectingScreen
    }
  }

  override fun channelRead0(ctx: ChannelHandlerContext, packet: Packets.Packet) {
    Main.logger().log("Client bound packet ${packet.type}")
    if (packet.direction == Packets.Packet.Direction.SERVER || packet.type.name.startsWith("SB_")) {
      ctx.fatal("Client got a server packet ${packet.type} direction ${packet.direction}")
      return
    }
    Main.inst().scheduler.executeSync { client.handleClientBoundPackets(packet) }
  }

  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    info = "Exception caught, ${cause.javaClass.simpleName}: ${cause.message}"
    cause.printStackTrace()
    ctx.close()
  }

  companion object {
    const val TAG = "CLIENT"
  }
}
