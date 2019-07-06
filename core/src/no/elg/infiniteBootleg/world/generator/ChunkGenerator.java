package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import org.jetbrains.annotations.NotNull;

/**
 * Generates chunks from world coordinates
 *
 * @author Elg
 */
public interface ChunkGenerator {

    /**
     * @param worldX
     *     World location
     *
     * @return The biome at the calculated location
     */
    @NotNull Biome getBiome(int worldX);

    /**
     * @param worldX
     *     World location
     *
     * @return The world height at this location
     */
    int getHeight(int worldX);

    /**
     * @param world
     *     The world to generate the chunk in
     * @param chunkX
     *     X coordinate of chunk in world to generate
     * @param chunkY
     *     Y coordinate of chunk in world to generate
     *
     * @return A chunk at the given offset in the given world
     */
    @NotNull Chunk generate(@NotNull World world, int chunkX, int chunkY);
}
