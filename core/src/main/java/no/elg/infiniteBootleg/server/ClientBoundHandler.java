package no.elg.infiniteBootleg.server;

import static no.elg.infiniteBootleg.server.TowardsClientPacketsHandlerKt.handleClientBoundPackets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.protobuf.Packets.Packet;
import no.elg.infiniteBootleg.screens.ConnectingScreen;
import org.jetbrains.annotations.NotNull;

/** @author Elg */
public class ClientBoundHandler extends SimpleChannelInboundHandler<Packet> {

  public static final String TAG = "CLIENT";
  private final ServerClient client;

  public ClientBoundHandler(ServerClient client) {
    this.client = client;
  }

  @Override
  public void channelActive(@NotNull ChannelHandlerContext ctx) {
    client.ctx = ctx;
  }

  @Override
  public void channelInactive(@NotNull ChannelHandlerContext ctx) {
    Main.inst()
        .getScheduler()
        .executeSync(
            () -> {
              var serverClient = ClientMain.inst().getServerClient();
              if (serverClient != null) {
                var sharedInformation = serverClient.getSharedInformation();
                if (sharedInformation != null) {
                  var task = sharedInformation.getHeartbeatTask();
                  if (task != null) {
                    task.cancel(false);
                  }
                }
              }
              ClientMain.inst().setScreen(ConnectingScreen.INSTANCE);
            });
  }

  @Override
  protected void channelRead0(@NotNull ChannelHandlerContext ctx, @NotNull Packet packet) {
    //        Main.logger().log("Client bound packet " + packet.getType());
    if (packet.getDirection() == Packet.Direction.SERVER
        || packet.getType().name().startsWith("SB_")) {
      PacketExtraKt.fatal(
          ctx,
          "Client got a server packet " + packet.getType() + " direction " + packet.getDirection());
      return;
    }
    Main.inst().getScheduler().executeSync(() -> handleClientBoundPackets(client, packet));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    ConnectingScreen.INSTANCE.setInfo(
        "Exception caught, " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
    cause.printStackTrace();
    ctx.close();
  }
}
