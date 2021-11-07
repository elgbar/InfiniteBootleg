package no.elg.infiniteBootleg.world.loader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.util.Random;
import java.util.UUID;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.util.ZipUtils;
import no.elg.infiniteBootleg.world.ServerWorld;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author Elg */
public class WorldLoader {

  public static final String WORLD_INFO_PATH = "world.dat";
  public static final String PLAYERS_PATH = "players";

  public static UUID getUUIDFromSeed(long seed) {
    byte[] uuidSeed = new byte[128];
    var random = new Random(seed);
    random.nextBytes(uuidSeed);
    return UUID.nameUUIDFromBytes(uuidSeed);
  }

  @Nullable
  private static World<?> loadWorld(long seed) {
    var uuid = getUUIDFromSeed(seed);
    var worldFolder = getWorldFolder(uuid);
    FileHandle worldZip = getWorldZip(worldFolder);

    if (!Settings.loadWorldFromDisk || worldFolder == null) {
      return null;
    }
    if (worldZip == null || !worldZip.exists()) {
      Main.logger().log("No world save found");
      return null;
    }
    worldFolder.deleteDirectory();
    ZipUtils.unzip(worldFolder, worldZip);
    return null;
  }

  @Nullable
  public static FileHandle getServerPlayerFile(@NotNull World<?> world, @NotNull UUID playerId) {
    FileHandle fileHandle = world.getWorldFolder();
    if (fileHandle != null) {
      fileHandle = fileHandle.child(PLAYERS_PATH).child(playerId.toString());
    }
    return fileHandle;
  }

  @NotNull
  public static Player getServerPlayer(@NotNull ServerWorld world, @NotNull UUID playerId) {
    FileHandle fileHandle = getServerPlayerFile(world, playerId);
    if (fileHandle != null && fileHandle.exists()) {
      try {
        var proto = ProtoWorld.Entity.parseFrom(fileHandle.readBytes());
        Player player = new Player(world, proto);
        if (!player.isInvalid()) {
          player.disableGravity();
          world.addEntity(player);
          return player;
        } else {
          Main.logger().error("SERVER", "Invalid player parsed");
          // fall through
        }
      } catch (Exception e) {
        Main.logger().error("SERVER", "Invalid entity protocol", e);
        // fall through
      }
    }
    // Invalid/non-existing player data
    final Player player = world.createNewPlayer(playerId);
    saveServerPlayer(player);
    return player;
  }

  public static void saveServerPlayer(@NotNull Player player) {
    FileHandle fileHandle = getServerPlayerFile(player.getWorld(), player.getUuid());
    if (fileHandle != null) {
      fileHandle.writeBytes(player.save().build().toByteArray(), false);
    }
  }

  public static FileHandle getWorldZip(FileHandle folder) {
    return folder != null ? folder.parent().child(folder.name() + ".zip") : null;
  }

  public static FileHandle getWorldFolder(@NotNull UUID uuid) {
    return Gdx.files.external(ClientMain.WORLD_FOLDER + uuid);
  }

  public static ChunkGenerator generatorFromProto(@NotNull ProtoWorld.World protoWorld) {
    return switch (protoWorld.getGenerator()) {
      case PERLIN, UNRECOGNIZED -> new PerlinChunkGenerator(protoWorld.getSeed());
      case FLAT -> new FlatChunkGenerator();
      case EMPTY -> new EmptyChunkGenerator();
    };
  }
}
