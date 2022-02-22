package no.elg.infiniteBootleg.server;

import static no.elg.infiniteBootleg.server.PacketExtraKt.fatal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.ServerMain;
import no.elg.infiniteBootleg.protobuf.Packets.Packet;
import no.elg.infiniteBootleg.screens.ConnectingScreen;
import org.jetbrains.annotations.NotNull;

/** @author Elg */
public class ServerBoundHandler extends SimpleChannelInboundHandler<Packet> {

  public static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
  public static final Map<Channel, ConnectionCredentials> clients = new ConcurrentHashMap<>();

  public static final String TAG = "SERVER";
  public static long packetsReceived;

  @Override
  protected void channelRead0(@NotNull ChannelHandlerContext ctx, @NotNull Packet packet) {
    //        Main.logger().log("Server bound packet " + packet.getType());
    packetsReceived++;
    if (packet.getDirection() == Packet.Direction.CLIENT
        || packet.getType().name().startsWith("CB_")) {
      fatal(
          ctx,
          "Server got a client packet " + packet.getType() + " direction " + packet.getDirection());
      return;
    } else if (packet.getType() != Packet.Type.SB_LOGIN) {
      var expectedSecret = clients.get(ctx.channel());
      if (expectedSecret == null) {
        fatal(ctx, "Unknown client");
        return;
      }
      if (!expectedSecret.getSecret().equals(packet.getSecret())) {
        fatal(ctx, "Invalid secret given");
        return;
      }
    }
    Main.inst()
        .getScheduler()
        .executeSync(() -> TowardsServerPacketsHandlerKt.handleServerBoundPackets(ctx, packet));
  }

  @Override
  public void channelActive(@NotNull final ChannelHandlerContext ctx) {
    channels.add(ctx.channel());
  }

  @Override
  public void channelInactive(@NotNull ChannelHandlerContext ctx) {
    channels.remove(ctx.channel());
    var client = clients.remove(ctx.channel());
    var playerId = client != null ? "" + client.getEntityUUID() : "<Unknown>";
    Main.logger()
        .debug(
            TAG,
            "client inactive (player "
                + playerId
                + ") (curr active "
                + clients.size()
                + " clients, "
                + channels.size()
                + " channels)");
    if (client != null) {
      ServerMain.inst().getServerWorld().disconnectPlayer(client.getEntityUUID(), false);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ConnectingScreen.INSTANCE.setInfo(
        "Exception caught, " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
    ctx.close();
  }
}
