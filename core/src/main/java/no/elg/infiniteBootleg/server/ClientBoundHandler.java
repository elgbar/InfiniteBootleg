package no.elg.infiniteBootleg.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.protobuf.Packets.Packet;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class ClientBoundHandler extends SimpleChannelInboundHandler<Packet> {

    @Override
    protected void channelRead0(@NotNull ChannelHandlerContext ctx, @NotNull Packet packet) throws Exception {
        if (packet.getDirection() == Packet.Direction.SERVER) {
            Main.inst().getConsoleLogger().error("CLIENT", "Client got a server packet");
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
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
