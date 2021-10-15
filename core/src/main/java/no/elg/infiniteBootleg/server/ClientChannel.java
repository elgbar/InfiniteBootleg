package no.elg.infiniteBootleg.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.protobuf.Packets;
import no.elg.infiniteBootleg.screens.ConnectingScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class ClientChannel {

    @Nullable
    private Channel channel;
    @NotNull
    private Client client;

    public ClientChannel(@NotNull Client client) { this.client = client; }

    public void connect(@NotNull String host, int port, @Nullable Runnable onConnect) throws InterruptedException {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap(); // (1)
            b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(@NotNull SocketChannel ch) {

                    final ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
                    pipeline.addLast("protobufDecoder", new ProtobufDecoder(Packets.Packet.getDefaultInstance()));
                    pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
                    pipeline.addLast("protobufEncoder", new ProtobufEncoder());
                    pipeline.addLast("ClientHandler", new ClientBoundHandler(client));
                }
            });

            // Start the client.
            try {
                channel = b.connect(host, port).sync().channel();
            } catch (Exception e) {
                ConnectingScreen.INSTANCE.setInfo(e.getClass().getSimpleName() + ": " + e.getMessage());
                return;
            }
            if (onConnect != null) {
                Main.inst().getScheduler().executeSync(onConnect);
            }
            // Wait until the connection is closed.
            channel.closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    @Nullable
    public Channel getChannel() {
        return channel;
    }

    @NotNull
    public Client getClient() {
        return client;
    }
}
