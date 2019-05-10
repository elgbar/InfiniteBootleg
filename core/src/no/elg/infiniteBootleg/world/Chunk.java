package no.elg.infiniteBootleg.world;

import com.google.common.base.Preconditions;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.blocks.Air;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.*;

/**
 * A piece of the world
 *
 * @author Elg
 */
public class Chunk implements Iterable<Block> {

    public static final int CHUNK_WIDTH = 32;
    public static final int CHUNK_HEIGHT = 32;

    private final World world;
    private final Location chunkPos;
    private final Block[][] blocks;
    private final int[] heightmap;
    private boolean loaded; //once unloaded it no longer is valid

    private boolean allAir;

    public Chunk(@Nullable World world, @NotNull Location chunkPos) {
        this.world = world;
        this.chunkPos = chunkPos;

        blocks = new Block[CHUNK_WIDTH][CHUNK_HEIGHT];
        heightmap = new int[CHUNK_WIDTH];
        allAir = true;
        loaded = true;
        IntStream.range(0, blocks.length).forEach(x -> Arrays.setAll(blocks[x], y -> new Air(x, y, world)));
    }

    /**
     * @param x
     *     The local x
     * @param y
     *     The local y
     *
     * @return A block from the relative coordinates
     */
    @NotNull
    public Block getBlock(int x, int y) {
        Preconditions.checkArgument(Util.isBetween(0, x, CHUNK_WIDTH),
                                    "Invalid position must be between 0 and " + CHUNK_WIDTH + ", but was" + x + ", " + y);
        Preconditions.checkArgument(Util.isBetween(0, y, CHUNK_HEIGHT),
                                    "Invalid position must be between 0 and " + CHUNK_HEIGHT + ", but was" + x + ", " + y);
        Preconditions.checkState(loaded, "Chunk is not loaded");
        return blocks[x][y];
    }

    public void setBlock(int x, int y, @NotNull Material material) {
        Preconditions.checkArgument(Util.isBetween(0, x, CHUNK_WIDTH),
                                    "Invalid position must be between 0 and " + CHUNK_WIDTH + ", but was" + x + ", " + y);
        Preconditions.checkArgument(Util.isBetween(0, y, CHUNK_HEIGHT),
                                    "Invalid position must be between 0 and " + CHUNK_HEIGHT + ", but was" + x + ", " + y);
        Preconditions.checkState(loaded, "Chunk is not loaded");

        blocks[x][y] = material.create(x, y, world);
        checkAllAir();
    }

    /**
     * test if all the blocks in this chunk has the material air
     */
    private void checkAllAir() {
        allAir = stream().allMatch(block -> block.getMaterial() == Material.AIR);
    }

    @NotNull
    public Block[][] getBlocks() {
        return blocks;
    }

    public boolean isAllAir() {
        return allAir;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void unload() {
        loaded = false;
    }

    @Nullable
    public World getWorld() {
        return world;
    }

    public Location getLocation() {
        return chunkPos;
    }

//    public Texture getTexture() {
//
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Chunk chunk = (Chunk) o;

        if (!Objects.equals(world, chunk.world)) { return false; }
        return chunkPos.equals(chunk.chunkPos);

    }

    @Override
    public int hashCode() {
        int result = world != null ? world.hashCode() : 0;
        result = 31 * result + chunkPos.hashCode();
        return result;
    }

    @NotNull
    @Override
    public Iterator<Block> iterator() {
        return new Iterator<Block>() {
            int x;
            int y;

            @Override
            public boolean hasNext() {
                return y < CHUNK_HEIGHT - 1 || x < CHUNK_WIDTH;
            }

            @Override
            public Block next() {
                if (x == CHUNK_WIDTH) {
                    x = 0;
                    y++;
                }
                return blocks[x++][y];
            }
        };
    }

    public Stream<Block> stream() {
        Spliterator<Block> spliterator =
            Spliterators.spliterator(iterator(), CHUNK_WIDTH * CHUNK_HEIGHT, SIZED | DISTINCT | NONNULL);
        return StreamSupport.stream(spliterator, false);
    }

    @Override
    public String toString() {
        return "Chunk{" + "world=" + world + ", chunkPos=" + chunkPos + '}';
    }
}
