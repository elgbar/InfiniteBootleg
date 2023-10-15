package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.screens.ConnectingScreen.info

/**
 * @author Elg
 */
class ClientBoundHandler(private val client: ServerClient) : SimpleChannelInboundHandler<Packets.Packet>() {
  override fun channelActive(ctx: ChannelHandlerContext) {
    client.ctx = ChannelHandlerContextWrapper("client->server", ctx)
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
    if (packet.direction == Packets.Packet.Direction.SERVER || packet.type.name.startsWith("SB_")) {
      ctx.fatal("Client got a server packet ${packet.type} direction ${packet.direction}")
      return
    }
    client.handleClientBoundPackets(packet)
  }

  @Deprecated("Deprecated in Java")
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    info = "Exception caught, ${cause.javaClass.simpleName}: ${cause.message}"
    Main.logger().error(TAG, "Exception in netty IO", cause)
    ctx.close()
  }

  companion object {
    const val TAG = "CLIENT"
  }
}
