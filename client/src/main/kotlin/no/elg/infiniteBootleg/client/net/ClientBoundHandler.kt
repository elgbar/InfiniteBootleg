package no.elg.infiniteBootleg.client.net

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import no.elg.infiniteBootleg.client.console.clientSideServerBoundMarker
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.ConnectingScreen
import no.elg.infiniteBootleg.core.net.ChannelHandlerContextWrapper
import no.elg.infiniteBootleg.core.net.ServerClient
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.protobuf.Packets

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class ClientBoundHandler(private val client: ServerClient) : SimpleChannelInboundHandler<Packets.Packet>() {
  override fun channelActive(ctx: ChannelHandlerContext) {
    client.channelActive(ChannelHandlerContextWrapper(clientSideServerBoundMarker, ctx))
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    launchOnMainSuspendable {
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
    client.handleClientBoundPackets(packet)
  }

  @Deprecated("Deprecated in Java")
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    ConnectingScreen.info = "Exception caught, ${cause.javaClass.simpleName}: ${cause.message}"
    logger.error(cause) { "Exception in netty IO" }
    ctx.close()
  }
}
