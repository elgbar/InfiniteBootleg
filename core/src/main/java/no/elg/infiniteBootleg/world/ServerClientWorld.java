package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.server.ServerClient;
import org.jetbrains.annotations.NotNull;

public class ServerClientWorld extends ClientWorld {

  @NotNull private final ServerClient serverClient;

  public ServerClientWorld(
      @NotNull ProtoWorld.World protoWorld, @NotNull ServerClient serverClient) {
    super(protoWorld);
    this.serverClient = serverClient;
  }

  @NotNull
  public ServerClient getServerClient() {
    return serverClient;
  }
}
