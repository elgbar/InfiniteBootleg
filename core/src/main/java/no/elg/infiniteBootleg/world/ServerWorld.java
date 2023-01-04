package no.elg.infiniteBootleg.world;

import static no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.PLAYER_KICKED;
import static no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason.PLAYER_QUIT;

import com.badlogic.ashley.core.Entity;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.render.HeadlessWorldRenderer;
import org.jetbrains.annotations.NotNull;

/**
 * World with extra functionality to handle multiple players
 *
 * @author Elg
 */
public class ServerWorld extends World {

  private final HeadlessWorldRenderer render = new HeadlessWorldRenderer(this);

  public ServerWorld(@NotNull ProtoWorld.World protoWorld) {
    super(protoWorld);
  }

  public ServerWorld(@NotNull ChunkGenerator generator, long seed, @NotNull String worldName) {
    super(generator, seed, worldName);
  }

  public void disconnectPlayer(@NotNull String uuid, boolean kicked) {
    var player = getEntity(uuid);
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
  protected void syncAddEntity(@NotNull Entity entity, boolean loadChunk) {
    super.syncAddEntity(entity, loadChunk);
    //    if (entity instanceof Player player) {
    //      render.addClient(
    //        player.getUuid(),
    //        new ServerClientChunksInView(CoordUtil.worldToChunk(player.getPosition())));
    //    }
    render.update();

    Main.inst()
        .getScheduler()
        .executeSync(
            () ->
                PacketExtraKt.broadcastToInView(
                    PacketExtraKt.clientBoundSpawnEntity(entity),
                    entity.getComponent(PositionComponent.class).getBlockX(),
                    entity.getComponent(PositionComponent.class).getBlockY(),
                    null));
  }

  @Override
  public void removeEntity(@NotNull Entity entity) {
    super.removeEntity(entity);
    //    if (entity instanceof Player player) {
    //      render.removeClient(player.getUuid());
    //    }
  }
}
