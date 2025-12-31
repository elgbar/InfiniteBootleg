package no.elg.infiniteBootleg.client.net

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import kotlinx.coroutines.CoroutineScope
import no.elg.infiniteBootleg.client.screens.ConnectingScreen
import no.elg.infiniteBootleg.core.net.ServerClient
import no.elg.infiniteBootleg.core.util.createEventLoopGroup
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.protobuf.Packets

/**
 * @author Elg
 */
class ClientChannel(val client: ServerClient) {
  lateinit var channel: Channel
    private set

  @Throws(InterruptedException::class)
  fun connect(host: String, port: Int, onConnect: suspend CoroutineScope.() -> Unit) {
    val workerGroup: EventLoopGroup = createEventLoopGroup()
    try {
      val b = Bootstrap()
      b.group(workerGroup)
      b.channel(NioSocketChannel::class.java)
      b.option(ChannelOption.SO_KEEPALIVE, true)
      b.option(ChannelOption.TCP_NODELAY, true)
      b.handler(
        object : ChannelInitializer<SocketChannel>() {
          public override fun initChannel(ch: SocketChannel) {
            val pipeline = ch.pipeline()
            pipeline.addLast("frameDecoder", ProtobufVarint32FrameDecoder())
            pipeline.addLast("protobufDecoder", ProtobufDecoder(Packets.Packet.getDefaultInstance()))
            pipeline.addLast("frameEncoder", ProtobufVarint32LengthFieldPrepender())
            pipeline.addLast("protobufEncoder", ProtobufEncoder())
            pipeline.addLast("ClientHandler", ClientBoundHandler(client))
          }
        }
      )

      // Start the client
      try {
        channel = b.connect(host, port).sync().channel()
      } catch (e: Exception) {
        ConnectingScreen.info = "${e.javaClass.simpleName}: ${e.message}"
        return
      }
      launchOnMainSuspendable(block = onConnect)
      if (::channel.isInitialized) {
        // Wait until the connection is closed
        channel.closeFuture().sync()
      }
    } finally {
      client.dispose()
      workerGroup.shutdownGracefully()
    }
  }
}
