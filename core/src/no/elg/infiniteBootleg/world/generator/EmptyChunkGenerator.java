package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import org.jetbrains.annotations.NotNull;

/**
 * Generate chunks that are always air
 *
 * @author Elg
 */
public class EmptyChunkGenerator implements ChunkGenerator {

    @Override
    public @NotNull Biome getBiome(int worldX) {
        return Biome.PLAINS;
    }

    @Override
    public int getHeight(int worldX) {
        return 0;
    }

    @Override
    public @NotNull Chunk generate(@NotNull World world, int chunkX, int chunkY) {
        return new Chunk(world, chunkX, chunkY);
    }
}
