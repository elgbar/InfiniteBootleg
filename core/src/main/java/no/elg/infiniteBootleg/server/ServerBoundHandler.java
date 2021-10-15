package no.elg.infiniteBootleg.server;

import static no.elg.infiniteBootleg.Main.inst;

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
public class ServerBoundHandler extends SimpleChannelInboundHandler<Packet> {

    public static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static final String TAG = "SERVER";

    @Override
    protected void channelRead0(@NotNull ChannelHandlerContext ctx, @NotNull Packet packet) {
        if (packet.getDirection() == Packet.Direction.CLIENT || packet.getType().name().startsWith("CB_")) {
            inst().getConsoleLogger().error(TAG, "Server got a client packet");
            return;
        }
        Main.inst().getConsoleLogger().log("Server bound packet " + packet.getType());
        TowardsServerPacketsHandlerKt.handleServerBoundPackets(ctx, packet);
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
