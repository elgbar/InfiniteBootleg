package no.elg.infiniteBootleg.world;

import com.google.common.base.Preconditions;
import no.elg.infiniteBootleg.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class Chunk {

    public static final int CHUNK_WIDTH = 64;
    public static final int CHUNK_HEIGHT = 256;

    private final World world;
    private final int offset;
    private final Block[][] blocks;

    /**
     * @param offset
     *     Where this chunk is in the world
     */
    public Chunk(@NotNull World world, int offset) {
        this.world = world;
        this.offset = offset;
        blocks = new Block[CHUNK_WIDTH][CHUNK_HEIGHT];
    }

    @NotNull
    public Block getBlock(int worldX, int worldY) {
        return getLocalBlock(Math.abs(worldX) % CHUNK_WIDTH, Math.abs(worldY) % CHUNK_HEIGHT);
    }

    /**
     * @param localX
     *     The local x
     * @param localY
     *     The local y
     *
     * @return A block from the relative coordinates
     */
    @NotNull
    public Block getLocalBlock(int localX, int localY) {
        Preconditions.checkArgument(Util.isBetween(0, localX, CHUNK_WIDTH));
        Preconditions.checkArgument(Util.isBetween(0, localY, CHUNK_HEIGHT));
        return blocks[localX][localY];
    }

    public void setBlock(int localX, int localY, @Nullable Block block) {
        Preconditions.checkArgument(Util.isBetween(0, localX, CHUNK_WIDTH));
        Preconditions.checkArgument(Util.isBetween(0, localY, CHUNK_HEIGHT));
        blocks[localX][localY] = block;
    }

    @NotNull
    public Block[][] getBlocks() {
        return blocks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Chunk chunk = (Chunk) o;

        if (offset != chunk.offset) { return false; }
        return world.equals(chunk.world);

    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + offset;
        return result;
    }
}
