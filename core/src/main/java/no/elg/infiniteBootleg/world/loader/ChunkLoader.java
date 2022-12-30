package no.elg.infiniteBootleg.world.loader;

import com.badlogic.gdx.files.FileHandle;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.server.ServerClient;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ChunkImpl;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handle saving and loading of chunks.
 *
 * <p>If a chunk is saved to disk then that chunk will be loaded (assuming {@link
 * Settings#loadWorldFromDisk} is {@code true}) Otherwise it will be generated with the given {@link
 * ChunkGenerator}
 *
 * @author Elg
 */
public class ChunkLoader {

  public static final String CHUNK_FOLDER = "chunks";
  private final World world;
  private final ChunkGenerator generator;

  public ChunkLoader(@NotNull World world, @NotNull ChunkGenerator generator) {
    this.world = world;
    this.generator = generator;
  }

  @Nullable
  public static FileHandle getChunkFile(@NotNull World world, int chunkX, int chunkY) {
    FileHandle worldFile = world.getWorldFolder();
    if (worldFile == null) {
      return null;
    }
    return worldFile.child(CHUNK_FOLDER + File.separator + chunkX + File.separator + chunkY);
  }

  /**
   * Load the chunk at the given chunk location
   *
   * @param chunkLoc The coordinates of the chunk (in chunk view)
   * @return The loaded chunk or null if something went wrong
   */
  @Nullable
  public synchronized Chunk load(long chunkLoc) {
    int chunkX = CoordUtil.decompactLocX(chunkLoc);
    int chunkY = CoordUtil.decompactLocY(chunkLoc);
    if (Main.isServerClient()) {
      ServerClient serverClient = ClientMain.inst().getServerClient();
      assert serverClient != null;
      serverClient.ctx.writeAndFlush(
          PacketExtraKt.serverBoundChunkRequestPacket(serverClient, chunkX, chunkY));
      return null;
    }

    if (existsOnDisk(chunkX, chunkY)) {
      ChunkImpl chunk = new ChunkImpl(world, chunkX, chunkY);
      FileHandle chunkFile = getChunkFile(world, chunkX, chunkY);
      if (chunkFile != null) {
        try {
          ProtoWorld.Chunk protoChunk = ProtoWorld.Chunk.parseFrom(chunkFile.readBytes());
          var loaded = load(chunk, protoChunk);
          if (loaded != null && loaded.isValid()) {

            Main.logger().log("Loaded chunk at " + CoordUtil.stringifyCompactLoc(chunkLoc));
            return loaded;
          }
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }
      }
      Main.logger()
          .warn(
              "Failed to load chunk "
                  + CoordUtil.stringifyCompactLoc(chunkLoc)
                  + " from existing file chunk file");
      // Failed to assemble, generate new chunk
    }
    Main.logger().log("Generating chunk at " + CoordUtil.stringifyCompactLoc(chunkLoc));
    Chunk generated = generator.generate(world, chunkX, chunkY);
    if (generated.isValid()) {
      return generated;
    }

    Main.logger().warn("Failed to generate chunk " + CoordUtil.stringifyCompactLoc(chunkLoc));
    return null;
  }

  @Nullable
  public Chunk clientLoad(@NotNull ProtoWorld.Chunk protoChunk) {
    ProtoWorld.Vector2i chunkPosition = protoChunk.getPosition();
    ChunkImpl chunk = new ChunkImpl(world, chunkPosition.getX(), chunkPosition.getY());
    return load(chunk, protoChunk);
  }

  @Nullable
  private Chunk load(@NotNull ChunkImpl chunk, @NotNull ProtoWorld.Chunk protoChunk) {
    if (chunk.load(protoChunk)) {
      chunk.finishLoading();
      return chunk;
    }
    return null;
  }

  /**
   * @param chunkX The y coordinate of the chunk (in chunk view)
   * @param chunkY The x coordinate of the chunk (in chunk view)
   * @return If a chunk at the given location exists
   */
  public boolean existsOnDisk(int chunkX, int chunkY) {
    if (!Settings.loadWorldFromDisk) {
      return false;
    }
    FileHandle chunkFile = getChunkFile(world, chunkX, chunkY);
    return chunkFile != null && chunkFile.exists();
  }

  //  @GuardedBy("no.elg.infiniteBootleg.world.World.chunksLock.writeLock()")
  public void save(@NotNull Chunk chunk) {
    if (Settings.loadWorldFromDisk && chunk.shouldSave() && chunk.isNotDisposed()) {
      // only save if valid and changed
      FileHandle fh = getChunkFile(world, chunk.getChunkX(), chunk.getChunkY());
      if (fh == null) {
        return;
      }
      fh.writeBytes(chunk.save().toByteArray(), false);
    }
  }

  public ChunkGenerator getGenerator() {
    return generator;
  }
}
