package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.google.common.base.Preconditions;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.*;
import static no.elg.infiniteBootleg.world.Material.AIR;

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
    private FrameBuffer fbo;
    private TextureRegion fboRegion;

    public Chunk(@Nullable World world, @NotNull Location chunkPos) {
        this.world = world;
        this.chunkPos = chunkPos;

        blocks = new Block[CHUNK_WIDTH][CHUNK_HEIGHT];
        heightmap = new int[CHUNK_WIDTH];
        allAir = true;
        loaded = true;
        updateTexture();
    }

    private void updateTexture() {
        if (Main.renderGraphic) {
            world.getRender().getChunkRenderer().queueRendering(this);
            if (fbo != null) {
                fbo.dispose();
                fbo = null;
                fboRegion = null;
            }
        }
    }

    /**
     * @param localX
     *     The local x ie a value between 0 and {@link #CHUNK_WIDTH}
     * @param localY
     *     The local y ie a value between 0 and {@link #CHUNK_HEIGHT}
     *
     * @return A block from the relative coordinates
     */
    @NotNull
    public Block getBlock(int localX, int localY) {
        Preconditions.checkArgument(Util.isBetween(0, localX, CHUNK_WIDTH),
                                    "Invalid position must be between 0 and " + CHUNK_WIDTH + ", but was" + localX + ", " +
                                    localY);
        Preconditions.checkArgument(Util.isBetween(0, localY, CHUNK_HEIGHT),
                                    "Invalid position must be between 0 and " + CHUNK_HEIGHT + ", but was" + localX + ", " +
                                    localY);
        Preconditions.checkState(loaded, "Chunk is not loaded");
        return blocks[localX][localY] == null ? AIR.create(localX, localY, world) : blocks[localX][localY];
    }

    /**
     * @param localX
     *     The local x ie a value between 0 and {@link #CHUNK_WIDTH}
     * @param localY
     *     The local y ie a value between 0 and {@link #CHUNK_HEIGHT}
     * @param material
     *     The material to place, if {@code null} it will effectively be {@link Material#AIR}
     */
    public void setBlock(int localX, int localY, @Nullable Material material) {
        Preconditions.checkArgument(Util.isBetween(0, localX, CHUNK_WIDTH),
                                    "Invalid position must be between 0 and " + CHUNK_WIDTH + ", but was" + localX + ", " +
                                    localY);
        Preconditions.checkArgument(Util.isBetween(0, localY, CHUNK_HEIGHT),
                                    "Invalid position must be between 0 and " + CHUNK_HEIGHT + ", but was" + localX + ", " +
                                    localY);
        Preconditions.checkState(loaded, "Chunk is not loaded");

        blocks[localX][localY] = material == null ? null : material.create(localX, localY, world);
        checkAllAir();
        updateTexture();
    }

    /**
     * test if all the blocks in this chunk has the material air
     */
    private void checkAllAir() {
        allAir = stream().allMatch(block -> block == null || block.getMaterial() == AIR);
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

    public TextureRegion getTexture() {
        if (isAllAir() || fbo == null) { return null; }
        return fboRegion;
    }

    public void setFbo(FrameBuffer fbo) {
        this.fbo = fbo;
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);
    }

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
