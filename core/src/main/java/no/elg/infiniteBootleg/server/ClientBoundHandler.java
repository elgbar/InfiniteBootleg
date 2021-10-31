package no.elg.infiniteBootleg.server;

import static no.elg.infiniteBootleg.server.TowardsClientPacketsHandlerKt.handleClientBoundPackets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.protobuf.Packets.Packet;
import no.elg.infiniteBootleg.screens.ConnectingScreen;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class ClientBoundHandler extends SimpleChannelInboundHandler<Packet> {

    public static final String TAG = "CLIENT";
    private final Client client;

    public ClientBoundHandler(Client client) { this.client = client; }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) {
        client.ctx = ctx;
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) {
        Main.inst().getScheduler().executeSync(() -> ClientMain.inst().setScreen(ConnectingScreen.INSTANCE));
    }

    @Override
    protected void channelRead0(@NotNull ChannelHandlerContext ctx, @NotNull Packet packet) {
        if (packet.getDirection() == Packet.Direction.SERVER || packet.getType().name().startsWith("SB_")) {
            Main.logger().error(TAG, "Client got a server packet");
            return;
        }
        Main.logger().log("Client bound packet " + packet.getType());
        handleClientBoundPackets(client, packet);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
