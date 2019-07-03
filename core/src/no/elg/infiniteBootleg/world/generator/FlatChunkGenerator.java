package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.biome.Biome;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class FlatChunkGenerator implements ChunkGenerator {

    @Override
    public @NotNull Biome getBiome(int worldX) {
        return Biome.PLAINS;
    }

    @NotNull
    @Override
    public Chunk generate(@NotNull World world, @NotNull Location chunkPos) {
        Chunk chunk = new Chunk(world, chunkPos);
        if (chunkPos.y < 0) {
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                    chunk.setBlock(x, y, Material.STONE, false);
                }
            }
        }
        chunk.updateTexture(false);
        return chunk;
    }
}
