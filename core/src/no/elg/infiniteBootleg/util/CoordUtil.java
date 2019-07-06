package no.elg.infiniteBootleg.util;

import no.elg.infiniteBootleg.world.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE_SHIFT;

/**
 * Translate between world(block) and chunk coordinates
 *
 * @author Elg
 */
public class CoordUtil {

    /**
     * @param worldLocation
     *     A location in world view
     *
     * @return The location in chunk view
     */
    @NotNull
    @Contract("_ -> new")
    public static Location worldToChunk(@NotNull Location worldLocation) {
        return new Location(worldToChunk(worldLocation.x), worldToChunk(worldLocation.y));
    }

    /**
     * @param chunkLocation
     *     A location in chunk view
     *
     * @return The location in world view
     */
    @NotNull
    @Contract("_ -> new")
    public static Location chunkToWorld(@NotNull Location chunkLocation) {
        return new Location(chunkToWorld(chunkLocation.x), chunkToWorld(chunkLocation.y));
    }

    /**
     * @param chunkLocation
     *     A location in chunk view
     * @param localX
     *     Local x offset from chunk position (no bounds checking)
     * @param localY
     *     Local y offset from chunk position (no bounds checking)
     *
     * @return The location in world view with the local offsets
     */
    @NotNull
    @Contract("_, _, _ -> new")
    public static Location chunkToWorld(@NotNull Location chunkLocation, int localX, int localY) {
        return new Location(chunkToWorld(chunkLocation.x, localX), chunkToWorld(chunkLocation.y, localY));
    }

    /**
     * @param worldCoord
     *     The world coordinate to convert
     *
     * @return The chunk coordinate the given coordinate is in
     */
    @Contract(pure = true)
    public static int worldToChunk(int worldCoord) {
        return worldCoord >> CHUNK_SIZE_SHIFT;
    }


    /**
     * Convert a chunk coordinate to world coordinate
     *
     * @param chunkCoord
     *     The chunk coordinate to convert
     *
     * @return The chunk coordinate in world view
     */
    @Contract(pure = true)
    public static int chunkToWorld(int chunkCoord) {
        return chunkCoord << CHUNK_SIZE_SHIFT;
    }

    /**
     * Convert a chunk coordinate to world coordinate with an offset within the chunk
     *
     * @param chunkCoord
     *     The chunk coordinate to convert
     * @param offset
     *     The offset within the chunk (no bounds checking)
     *
     * @return The chunk coordinate in world view plus the offset
     */
    @Contract(pure = true)
    public static int chunkToWorld(int chunkCoord, int offset) {
        return chunkToWorld(chunkCoord) + offset;
    }

    /**
     * Calculate the offset the given world coordinate have in its chunk
     *
     * @param worldCoord
     *     A coordinate in world view
     *
     * @return The local  coordinate given coordinate have in chunk view
     */
    @Contract(pure = true)
    public static int calculateOffset(int worldCoord) {
        return worldCoord - chunkToWorld(worldToChunk(worldCoord));
    }
}
