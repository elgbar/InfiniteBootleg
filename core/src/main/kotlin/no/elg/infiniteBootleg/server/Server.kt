package no.elg.infiniteBootleg.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.protobuf.Packets

/**
 * @author Elg
 */
class Server {
  @Throws(InterruptedException::class)
  fun start() {
    val bossGroup: EventLoopGroup = NioEventLoopGroup()
    val workerGroup: EventLoopGroup = NioEventLoopGroup()
    try {
      val b = ServerBootstrap()
      b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel::class.java)
        .childHandler(
          object : ChannelInitializer<SocketChannel>() {
            public override fun initChannel(ch: SocketChannel) {
              val pipeline = ch.pipeline()
              pipeline.addLast("frameDecoder", ProtobufVarint32FrameDecoder())
              pipeline.addLast("protobufDecoder", ProtobufDecoder(Packets.Packet.getDefaultInstance()))
              pipeline.addLast("frameEncoder", ProtobufVarint32LengthFieldPrepender())
              pipeline.addLast("protobufEncoder", ProtobufEncoder())
              pipeline.addLast("ServerHandler", ServerBoundHandler())
            }
          }
        )
        .option(ChannelOption.SO_BACKLOG, 128)
        .childOption(ChannelOption.SO_KEEPALIVE, true)

      // Bind and start to accept incoming connections.
      val channelFuture = b.bind(Settings.port).sync()

      // Wait until the server socket is closed
      // In this example, this does not happen, but you can do that to gracefully shut down your server
      channelFuture.channel().closeFuture().sync()
    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}
