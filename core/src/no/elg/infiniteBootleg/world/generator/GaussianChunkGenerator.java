package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;
import static no.elg.infiniteBootleg.world.Chunk.CHUNK_WIDTH;

public class GaussianChunkGenerator implements ChunkGenerator {

    int lastRight = CHUNK_HEIGHT / 2;
    int lastLeft = CHUNK_HEIGHT / 2;

    @Override
    public @NotNull Chunk generate(@Nullable World world, @NotNull Location chunkPos, @NotNull Random random) {
        Chunk chunk = new Chunk(world, chunkPos);
        if (chunkPos.y > 1) { return chunk; }
        else if (chunkPos.y < 1) {
            for (int x = 0; x < CHUNK_WIDTH; x++) {
                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    chunk.setBlock(x, y, Material.STONE, false);
                }
            }
            return chunk;
        }
        for (int x = 0; x < Chunk.CHUNK_WIDTH; x++) {
            lastRight += (int) Math.round(random.nextGaussian());
            lastRight = Math.min(CHUNK_HEIGHT, Math.max(0, lastRight));
            fillUpTo(chunk, x, lastRight, Material.STONE);
        }
        chunk.update(false);
        return chunk;
    }

    private void fillUpTo(Chunk chunk, int x, int y, Material mat) {
        for (int dy = 0; dy < y; dy++) {
            chunk.setBlock(x, dy, mat, false);
        }
    }
}
