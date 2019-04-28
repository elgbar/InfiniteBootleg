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

    public static final int CHUNK_WIDTH = 64;
    public static final int CHUNK_HEIGHT = 256;

    private final World world;
    private final int offset;
    private final Block[][] blocks;

    /**
     * @param offset
     *     Where this chunk is in the world
     */
    public Chunk(int offset, @Nullable World world) {
        this.world = world;
        this.offset = offset;
        blocks = new Block[CHUNK_WIDTH][CHUNK_HEIGHT];
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
        Preconditions.checkArgument(Util.isBetween(0, x, CHUNK_WIDTH));
        Preconditions.checkArgument(Util.isBetween(0, y, CHUNK_HEIGHT));
        return blocks[x][y];
    }

    public void setBlock(int x, int y, @NotNull Block block) {
        Preconditions.checkArgument(Util.isBetween(0, x, CHUNK_WIDTH));
        Preconditions.checkArgument(Util.isBetween(0, y, CHUNK_HEIGHT));
        blocks[x][y] = block;
    }

    @NotNull
    public Block[][] getBlocks() {
        return blocks;
    }

    @Nullable
    public World getWorld() {
        return world;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Chunk chunk = (Chunk) o;

        if (offset != chunk.offset) { return false; }
        return Objects.equals(world, chunk.world);

    }

    @Override
    public int hashCode() {
        int result = world != null ? world.hashCode() : 0;
        result = 31 * result + offset;
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
}
