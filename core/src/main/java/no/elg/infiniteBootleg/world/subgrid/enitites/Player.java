package no.elg.infiniteBootleg.world.subgrid.enitites;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction.PUSH_UP;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.google.common.base.Preconditions;
import java.util.UUID;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.input.KeyboardControls;
import no.elg.infiniteBootleg.input.WorldInputHandler;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Deprecated
public class Player extends LivingEntity {

  public static final String PLAYER_REGION_NAME = "player";

  @Nullable private static final TextureRegion TEXTURE_REGION;
  @Nullable private EntityControls controls;

  public Player(@NotNull World world, @NotNull ProtoWorld.Entity protoEntity) {
    super(world, protoEntity);
    if (isDisposed()) {
      return;
    }

    Preconditions.checkArgument(protoEntity.hasPlayer(), "Player does not contain player data");
    final ProtoWorld.Entity.Player protoPlayer = protoEntity.getPlayer();
    setLookDeg(protoPlayer.getTorchAngleDeg());

    if (protoPlayer.getControlled() && Main.isSingleplayer()) {
      Main.inst()
          .getScheduler()
          .executeSync(
              () -> {
                if (!isDisposed()) {
                  ClientMain.inst().setPlayer(this);
                }
              });
    }
  }

  public Player(@NotNull World world, float worldX, float worldY) {
    this(world, worldX, worldY, UUID.randomUUID());
  }

  public Player(@NotNull World world, float worldX, float worldY, @NotNull UUID uuid) {
    super(world, worldX, worldY, uuid);
    if (isDisposed()) {
      return;
    }
    if (Settings.client) {
      ClientMain.inst().setPlayer(this);
    }
  }

  static {
    if (Settings.client) {
      TEXTURE_REGION =
          new TextureRegion(ClientMain.inst().getEntityAtlas().findRegion(PLAYER_REGION_NAME));
    } else {
      TEXTURE_REGION = null;
    }
  }

  public synchronized void giveControls() {
    if (Main.isServer()) {
      return;
    }
    ClientWorld world = (ClientWorld) getWorld();
    if (controls == null) {
      Main.logger().debug("PLR", "Giving control to " + hudDebug());
      if (Main.isServerClient()
          && !getUuid().equals(ClientMain.inst().getServerClient().getUuid())) {
        throw new IllegalCallerException("Cannot give controls to others than " + getUuid());
      }
      controls = new KeyboardControls(world.getRender(), this);
      ClientMain.inst().getInputMultiplexer().addProcessor(controls);
      final WorldInputHandler input = world.getInput();
      input.setFollowing(this);
    } else {
      Main.logger()
          .warn("PLR", "Tried to give control to a player already with control " + hudDebug());
    }
  }

  public synchronized void removeControls() {
    if (Main.isServer()) {
      return;
    }
    if (controls != null) {
      Main.logger().debug("PLR", "Removing control from " + hudDebug());
      controls.dispose();
      controls = null;
    } else {
      Main.logger()
          .warn("PLR", "Tried to remove control from a player without control " + hudDebug());
    }
  }

  @Override
  public synchronized boolean hasControls() {
    return controls != null;
  }

  @Override
  public TextureRegion getTextureRegion() {
    return TEXTURE_REGION;
  }

  @Override
  public int getWidth() {
    return 2 * BLOCK_SIZE - 1;
  }

  @Override
  public int getHeight() {
    return 4 * BLOCK_SIZE - 1;
  }

  public void toggleTorch() {
    // TODO
  }

  @Override
  public ProtoWorld.Entity.@NotNull Builder save() {
    final ProtoWorld.Entity.Builder builder = super.save();
    final ProtoWorld.Entity.Player.Builder playerBuilder = ProtoWorld.Entity.Player.newBuilder();

    playerBuilder.setControlled(!Main.isMultiplayer() && hasControls());

    builder.setPlayer(playerBuilder.build());
    return builder;
  }

  @Override
  public void dispose() {
    if (isDisposed()) {
      return;
    }
    super.dispose();
    if (hasControls()) {
      removeControls();
    }
  }

  @Override
  public InvalidSpawnAction invalidSpawnLocationAction() {
    return PUSH_UP;
  }

  @Override
  public void tick() {
    super.tick();

    //    if (Main.isServerClient() && hasControls()) {
    //      final ServerClient client = ClientMain.inst().getServerClient();
    //      if (client != null) {
    //        client.ctx.writeAndFlush(PacketExtraKt.serverBoundMoveEntityPacket(client, this));
    //      }
    //    }
  }

  @Override
  @Nullable
  public EntityControls getControls() {
    return controls;
  }

  @Override
  protected @NotNull ProtoWorld.Entity.EntityType getEntityType() {
    return ProtoWorld.Entity.EntityType.PLAYER;
  }
}
