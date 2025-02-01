package no.elg.infiniteBootleg.net

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import no.elg.infiniteBootleg.console.clientSideServerBoundMarker
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.util.launchOnMain

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class ClientBoundHandler(private val client: ServerClient) : SimpleChannelInboundHandler<Packets.Packet>() {
  override fun channelActive(ctx: ChannelHandlerContext) {
    client.ctx = ChannelHandlerContextWrapper(clientSideServerBoundMarker, ctx)
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    launchOnMain {
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
      client.ctx.fatal("Client got a server packet ${packet.type} direction ${packet.direction}")
      return
    }
    client.handleClientBoundPackets(packet)
  }

  @Deprecated("Deprecated in Java")
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    ConnectingScreen.info = "Exception caught, ${cause.javaClass.simpleName}: ${cause.message}"
    logger.error(cause) { "Exception in netty IO" }
    ctx.close()
  }
}
