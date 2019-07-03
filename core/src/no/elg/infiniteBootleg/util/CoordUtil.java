package no.elg.infiniteBootleg.util;

import no.elg.infiniteBootleg.world.Location;

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
    public static Location worldToChunk(Location worldLocation) {
        return new Location(worldToChunk(worldLocation.x), worldToChunk(worldLocation.y));
    }

    /**
     * @param chunkLocation
     *     A location in chunk view
     *
     * @return The location in world view
     */
    public static Location chunkToWorld(Location chunkLocation) {
        return new Location(chunkToWorld(chunkLocation.x), chunkToWorld(chunkLocation.y));
    }

    /**
     * @param worldCoord
     *     The world coordinate to convert
     *
     * @return The chunk coordinate the given coordinate is in
     */
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
    public static int chunkToWorld(int chunkCoord) {
        return chunkToWorld(chunkCoord, 0);
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
    public static int chunkToWorld(int chunkCoord, int offset) {
        return (chunkCoord << CHUNK_SIZE_SHIFT) + offset;
    }
}
