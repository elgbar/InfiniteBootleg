package no.elg.infiniteBootleg.util;

import no.elg.infiniteBootleg.world.Location;

import static no.elg.infiniteBootleg.world.World.CHUNK_WIDTH_SHIFT;

/**
 * Translate between world and chunk coordinates
 *
 * @author Elg
 */
public class CoordUtil {

    public static Location worldToChunk(Location worldLocation) {
        return new Location(worldToChunk(worldLocation.x), worldToChunk(worldLocation.y));
    }

    public static Location chunkToWorld(Location chunkLocation) {
        return new Location(chunkToWorld(chunkLocation.x), chunkToWorld(chunkLocation.y));
    }

    public static int worldToChunk(int worldCoord) {
        return worldCoord >> CHUNK_WIDTH_SHIFT;
    }

    public static int chunkToWorld(int chunkCoord) {
        return chunkCoord << CHUNK_WIDTH_SHIFT;
    }
}
