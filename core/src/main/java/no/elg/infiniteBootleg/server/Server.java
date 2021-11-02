package no.elg.infiniteBootleg.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.Packets;
import org.jetbrains.annotations.NotNull;

/** @author Elg */
public class Server {

  public void start() throws InterruptedException {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap b = new ServerBootstrap(); // (2)
      b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class) // (3)
          .childHandler(
              new ChannelInitializer<SocketChannel>() { // (4)
                @Override
                public void initChannel(@NotNull SocketChannel ch) {

                  final ChannelPipeline pipeline = ch.pipeline();
                  pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
                  pipeline.addLast(
                      "protobufDecoder", new ProtobufDecoder(Packets.Packet.getDefaultInstance()));
                  pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
                  pipeline.addLast("protobufEncoder", new ProtobufEncoder());
                  pipeline.addLast("ServerHandler", new ServerBoundHandler());
                }
              })
          .option(ChannelOption.SO_BACKLOG, 128) // (5)
          .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

      // Bind and start to accept incoming connections.
      ChannelFuture f = b.bind(Settings.port).sync(); // (7)

      // Wait until the server socket is closed.
      // In this example, this does not happen, but you can do that to gracefully
      // shut down your server.
      f.channel().closeFuture().sync();
    } finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
    }
  }
}
