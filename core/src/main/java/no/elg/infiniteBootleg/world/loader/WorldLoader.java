package no.elg.infiniteBootleg.world.loader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.util.UUID;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.world.ServerWorld;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public final class WorldLoader {

  public static final String LOCK_FILE_NAME = ".locked";
  private static final Object WORLD_LOCK_LOCK = new Object();

  private WorldLoader() {}

  public static final String WORLD_INFO_PATH = "world.dat";
  public static final String PLAYERS_PATH = "players";

  @Nullable
  public static FileHandle getServerPlayerFile(@NotNull World world, @NotNull UUID playerId) {
    FileHandle fileHandle = world.getWorldFolder();
    if (fileHandle != null) {
      fileHandle = fileHandle.child(PLAYERS_PATH).child(playerId.toString());
    }
    return fileHandle;
  }

  @NotNull
  public static Player spawnServerPlayer(@NotNull ServerWorld world, @NotNull UUID playerId) {
    FileHandle fileHandle = getServerPlayerFile(world, playerId);
    if (fileHandle != null && fileHandle.exists()) {
      try {
        var proto = ProtoWorld.Entity.parseFrom(fileHandle.readBytes());
        Player player = new Player(world, proto);
        player.disableGravity();
        world.addEntity(player);
        if (!player.isDisposed()) {
          Main.logger().debug("SERVER", "Loading persisted player profile for " + playerId);
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
    Main.logger().debug("SERVER", "Creating fresh player profile for " + playerId);
    // Invalid/non-existing player data
    Player player = world.createNewPlayer(playerId);
    saveServerPlayer(player);
    return player;
  }

  public static void saveServerPlayer(@NotNull Player player) {
    FileHandle fileHandle = getServerPlayerFile(player.getWorld(), player.getUuid());
    if (fileHandle != null) {
      try {
        fileHandle.writeBytes(player.save().build().toByteArray(), false);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @NotNull
  public static FileHandle getWorldFolder(@NotNull UUID uuid) {
    return Gdx.files.external(Main.WORLD_FOLDER + uuid);
  }

  public static FileHandle getWorldLockFile(@NotNull UUID uuid) {
    return getWorldFolder(uuid).child(LOCK_FILE_NAME);
  }

  public static boolean canWriteToWorld(@NotNull UUID uuid) {
    synchronized (WORLD_LOCK_LOCK) {
      if (Settings.ignoreWorldLock) {
        return true;
      }
      FileHandle worldLockFile = getWorldLockFile(uuid);
      if (worldLockFile.isDirectory()) {
        // Invalid format, allow writing
        Main.logger().warn("World lock file for " + uuid + " was a directory");
        worldLockFile.deleteDirectory();
        return true;
      }
      if (!worldLockFile.exists()) {
        // No lock file, we can ofc write!
        return true;
      }
      String lockInfo = worldLockFile.readString();
      long lockPID;
      try {
        lockPID = Long.parseLong(lockInfo);
      } catch (NumberFormatException e) {
        Main.logger()
            .warn(
                "World lock file for " + uuid + " did not contain a valid pid, read: " + lockInfo);
        worldLockFile.delete();
        // Invalid pid, allow writing
        return true;
      }

      if (lockPID == ProcessHandle.current().pid()) {
        // We own the lock
        return true;
      }

      if (ProcessHandle.of(lockPID).isEmpty()) {
        // If there is no process with the read pid, it was probably left from an old instance
        Main.logger()
            .warn(
                "World lock file for "
                    + uuid
                    + " still existed for a non-existing process, an old lock file found");
        worldLockFile.delete();
        return true;
      }
      return false;
    }
  }

  public static boolean writeLockFile(@NotNull UUID uuid) {
    synchronized (WORLD_LOCK_LOCK) {
      if (!canWriteToWorld(uuid)) {
        return false;
      }
      FileHandle worldLockFile = getWorldLockFile(uuid);
      worldLockFile.writeString(ProcessHandle.current().pid() + "", false);
      return true;
    }
  }

  public static boolean deleteLockFile(@NotNull UUID uuid) {
    synchronized (WORLD_LOCK_LOCK) {
      if (!canWriteToWorld(uuid)) {
        return false;
      }
      getWorldLockFile(uuid).delete();
      return true;
    }
  }

  @NotNull
  public static ChunkGenerator generatorFromProto(@NotNull ProtoWorld.World protoWorld) {
    return switch (protoWorld.getGenerator()) {
      case PERLIN, UNRECOGNIZED -> new PerlinChunkGenerator(protoWorld.getSeed());
      case FLAT -> new FlatChunkGenerator();
      case EMPTY -> new EmptyChunkGenerator();
    };
  }
}
