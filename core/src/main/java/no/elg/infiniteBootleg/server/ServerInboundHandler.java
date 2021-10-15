package no.elg.infiniteBootleg.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.protobuf.Packets.Packet;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class ServerInboundHandler extends SimpleChannelInboundHandler<Packet> {

    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(@NotNull ChannelHandlerContext ctx, @NotNull Packet packet) throws Exception {
        if (packet.getDirection() == Packet.Direction.CLIENT) {
            Main.inst().getConsoleLogger().error("SERVER", "Server got a client packet");
            return;
        }
        System.out.println(packet.getType());
//        for (Channel c : channels) {
//            if (c != ctx.channel()) {
//                c.writeAndFlush("[" + ctx.channel().remoteAddress() + "] " + msg + '\n');
//            }
//            else {
//                c.writeAndFlush("[you] " + msg + '\n');
//            }
//        }
//
//        // Close the connection if the client has sent 'bye'.
//        if ("bye".equals(msg.toLowerCase())) {
//            ctx.close();
//        }
    }

    @Override
    public void channelActive(@NotNull final ChannelHandlerContext ctx) {
        channels.add(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
