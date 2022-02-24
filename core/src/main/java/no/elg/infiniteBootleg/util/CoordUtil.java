package no.elg.infiniteBootleg.util;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE;

import com.badlogic.gdx.math.Vector2;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Translate between world(block) and chunk coordinates
 *
 * @author Elg
 */
public class CoordUtil {

  /**
   * @param worldLocation A location in world view
   * @return The location in chunk view
   */
  @NotNull
  @Contract("_ -> new")
  public static Location worldToChunk(@NotNull Location worldLocation) {
    return new Location(worldToChunk(worldLocation.x), worldToChunk(worldLocation.y));
  }

  /**
   * @param worldLocation A location in world view
   * @return The location in chunk view
   */
  @NotNull
  @Contract("_ -> new")
  public static Location worldToChunk(@NotNull Vector2 worldLocation) {
    return new Location(worldToChunk((int) worldLocation.x), worldToChunk((int) worldLocation.y));
  }

  /**
   * @param worldCoord The world coordinate to convert
   * @return The chunk coordinate the given coordinate is in
   */
  @Contract(pure = true)
  public static int worldToChunk(int worldCoord) {
    return worldCoord >> Chunk.CHUNK_SIZE_SHIFT;
  }

  /**
   * @param worldCoord The world coordinate to convert
   * @return The chunk coordinate the given coordinate is in
   */
  @Contract(pure = true)
  public static int worldToChunk(float worldCoord) {
    return worldToChunk((int) worldCoord);
  }

  /**
   * @param chunkLocation A location in chunk view
   * @return The location in world view
   */
  @NotNull
  @Contract("_ -> new")
  public static Location chunkToWorld(@NotNull Location chunkLocation) {
    return new Location(chunkToWorld(chunkLocation.x), chunkToWorld(chunkLocation.y));
  }

  /**
   * Convert a chunk coordinate to world coordinate
   *
   * @param chunkCoord The chunk coordinate to convert
   * @return The chunk coordinate in world view
   */
  @Contract(pure = true)
  public static int chunkToWorld(int chunkCoord) {
    return chunkCoord << Chunk.CHUNK_SIZE_SHIFT;
  }

  /**
   * Convert a chunk coordinate to world coordinate
   *
   * @param chunkCoord The chunk coordinate to convert
   * @return The chunk coordinate in world view
   */
  @Contract(pure = true)
  public static int chunkToWorld(float chunkCoord) {
    return chunkToWorld((int) chunkCoord);
  }

  /**
   * @param chunkLocation A location in chunk view
   * @param localX Local x offset from chunk position (no bounds checking)
   * @param localY Local y offset from chunk position (no bounds checking)
   * @return The location in world view with the local offsets
   */
  @NotNull
  @Contract("_, _, _ -> new")
  public static Location chunkToWorld(@NotNull Location chunkLocation, int localX, int localY) {
    return new Location(
        chunkToWorld(chunkLocation.x, localX), chunkToWorld(chunkLocation.y, localY));
  }

  /**
   * Convert a chunk coordinate to world coordinate with an offset within the chunk
   *
   * @param chunkCoord The chunk coordinate to convert
   * @param offset The offset within the chunk (no bounds checking)
   * @return The chunk coordinate in world view plus the offset
   */
  @Contract(pure = true)
  public static int chunkToWorld(int chunkCoord, int offset) {
    return chunkToWorld(chunkCoord) + offset;
  }

  /**
   * Calculate the offset the given world coordinate have in its chunk
   *
   * @param worldCoord A coordinate in world view
   * @return The local coordinate given coordinate have in chunk view
   */
  @Contract(pure = true)
  public static int chunkOffset(int worldCoord) {
    return worldCoord - chunkToWorld(worldToChunk(worldCoord));
  }

  /**
   * @param localX The chunk local x coordinate
   * @param localY The chunk local y coordinate
   * @return If given x and y are both between 0 (inclusive) and {@link Chunk#CHUNK_SIZE}
   *     (exclusive)
   */
  @Contract(pure = true)
  public static boolean isInsideChunk(int localX, int localY) {
    return localX >= 0 && localX < CHUNK_SIZE && localY >= 0 && localY < CHUNK_SIZE;
  }

  /**
   * @param worldX The world x coordinate
   * @param worldY The world y coordinate
   * @return The chunk location for the given world coordinates
   */
  @Contract(pure = true)
  public static @NotNull Location worldXYtoChunkLoc(int worldX, int worldY) {
    return new Location(worldToChunk(worldX), worldToChunk(worldY));
  }
}
