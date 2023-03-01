package no.elg.infiniteBootleg.server

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.screens.ConnectingScreen.info

/**
 * @author Elg
 */
class ClientChannel(val client: ServerClient) {
  lateinit var channel: Channel
    private set

  @Throws(InterruptedException::class)
  fun connect(host: String, port: Int, onConnect: Runnable?) {
    val workerGroup: EventLoopGroup = NioEventLoopGroup()
    try {
      val b = Bootstrap()
      b.group(workerGroup)
      b.channel(NioSocketChannel::class.java)
      b.option(ChannelOption.SO_KEEPALIVE, true)
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
        info = "${e.javaClass.simpleName}: ${e.message}"
        return
      }
      if (onConnect != null) {
        Main.inst().scheduler.executeSync(onConnect)
      }
      if (::channel.isInitialized) {
        // Wait until the connection is closed
        channel.closeFuture().sync()
      }
    } finally {
      workerGroup.shutdownGracefully()
    }
  }
}
