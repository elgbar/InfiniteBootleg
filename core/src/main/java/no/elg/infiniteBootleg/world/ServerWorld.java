package no.elg.infiniteBootleg.world;

import static no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.PLAYER_KICKED;
import static no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.PLAYER_QUIT;

import java.util.UUID;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.render.ChunksInView;
import no.elg.infiniteBootleg.world.render.HeadlessWorldRenderer;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.NotNull;

/**
 * World with extra functionality to handle multiple players
 *
 * @author Elg
 */
public class ServerWorld extends World<HeadlessWorldRenderer> {

  public ServerWorld(@NotNull ProtoWorld.World protoWorld) {
    super(protoWorld);
  }

  public ServerWorld(@NotNull ChunkGenerator generator, long seed, @NotNull String worldName) {
    super(generator, seed, worldName);
  }

  private final HeadlessWorldRenderer render;

  {
    render = new HeadlessWorldRenderer(this);
  }

  public void disconnectPlayer(@NotNull UUID uuid, boolean kicked) {
    final Player player = getPlayer(uuid);
    if (player != null) {
      removeEntity(player, kicked ? PLAYER_KICKED : PLAYER_QUIT);
    } else {
      Main.logger().warn("Failed to find player " + uuid + " to remove");
    }
  }

  @Override
  @NotNull
  public HeadlessWorldRenderer getRender() {
    return render;
  }

  @Override
  public void addEntity(@NotNull Entity entity) {
    super.addEntity(entity);
    if (entity instanceof Player player) {
      render.getViewingChunks().put(player.getUuid(), new ChunksInView());
    }

    Main.inst()
        .getScheduler()
        .executeSync(
            () -> PacketExtraKt.broadcast(PacketExtraKt.clientBoundSpawnEntity(entity), null));
  }

  @Override
  public void removeEntity(@NotNull Entity entity) {
    super.removeEntity(entity);
    if (entity instanceof Player player) {
      render.getViewingChunks().remove(player.getUuid());
    }
  }
}
