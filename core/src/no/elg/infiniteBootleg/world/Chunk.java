package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.google.common.base.Preconditions;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.render.Updatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.*;
import static no.elg.infiniteBootleg.world.Material.AIR;

/**
 * A piece of the world
 *
 * @author Elg
 */
public class Chunk implements Iterable<Block>, Updatable, Disposable {

    public static final int CHUNK_WIDTH = 32;
    public static final int CHUNK_HEIGHT = 32;

    private final World world;
    private final Location chunkPos;
    private final Block[][] blocks;
    private final int[] heightmap;

    private final List<Updatable> updatableBlocks;

    private boolean dirty; //if texture/allair needs to be updated
    private boolean prioritize; //if this chunk should be prioritized to be updated
    private boolean loaded; //once unloaded it no longer is valid
    private boolean canUnload;

    private long lastViewedTick;
    private boolean allAir;
    private FrameBuffer fbo;
    private TextureRegion fboRegion;

    public Chunk(@NotNull World world, @NotNull Location chunkPos) {
        this.world = world;
        this.chunkPos = chunkPos;

        blocks = new Block[CHUNK_WIDTH][CHUNK_HEIGHT];
        heightmap = new int[CHUNK_WIDTH];
        updatableBlocks = new ArrayList<>();

        allAir = true;
        loaded = true;

        dirty = true;
        prioritize = false;
    }

    public void updateTextureNow() {
        //test if all the blocks in this chunk has the material air
        allAir = stream().allMatch(block -> block == null || block.getMaterial() == AIR);
        if (Main.renderGraphic) {
            world.getRender().getChunkRenderer().queueRendering(this, prioritize);
            prioritize = false;
        }
        dirty = false;
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
        Preconditions.checkState(loaded, "Chunk is not loaded");
        if (blocks[localX][localY] == null) {
            setBlock(localX, localY, AIR, true);
        }
        return blocks[localX][localY];
    }

    /**
     * @param localX
     *     The local x ie a value between 0 and {@link #CHUNK_WIDTH}
     * @param localY
     *     The local y ie a value between 0 and {@link #CHUNK_HEIGHT}
     * @param material
     *     The material to place, if {@code null} it will effectively be {@link Material#AIR}
     * @param update
     */
    public void setBlock(int localX, int localY, @Nullable Material material, boolean update) {
        Preconditions.checkState(loaded, "Chunk is not loaded");

        Block currBlock = blocks[localX][localY];

        if ((currBlock == null && material == null) || (currBlock != null && currBlock.getMaterial() == material)) {
            return;
        }
        if (material == null) {
            blocks[localX][localY] = null;
        }
        else {
            Block newBlock = material.create(localX, localY, world);
            blocks[localX][localY] = newBlock;

            if (currBlock instanceof Updatable && !(newBlock instanceof Updatable)) {
                updatableBlocks.remove(currBlock);
            }
            else if (newBlock instanceof Updatable) {
                updatableBlocks.add((Updatable) newBlock);
            }
        }

        if (update) {
            dirty = true;
            prioritize = true;
        }
    }

    /**
     * Force update of this chunk's texture and invariants
     *
     * @param prioritize
     *     If this chunk should be prioritized when rendering
     */
    public void updateTexture(boolean prioritize) {
        dirty = true;
        this.prioritize = prioritize;
    }

    public TextureRegion getTexture() {
        lastViewedTick = world.getWorldTicker().getTickId();
        if (dirty) {
            updateTextureNow();
        }
        if (fbo == null) { return null; }
        return fboRegion;
    }

    public void setFbo(FrameBuffer fbo) {
        if (this.fbo != null) {
            this.fbo.dispose();
            this.fbo = null;
            this.fboRegion = null;
        }
        this.fbo = fbo;
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);
    }

    @Override
    public void update() {
        for (Updatable block : updatableBlocks) {
            block.update();
        }
    }

    @NotNull
    public Block[][] getBlocks() {
        return blocks;
    }

    public boolean isAllAir() {
        if (dirty) {
            updateTextureNow();
        }
        return allAir;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /**
     * @return If the chunk was unloaded
     */
    public boolean unload() {
        if (canUnload) {
            loaded = false;
            dispose();
            return true;
        }
        return false;
    }

    public void allowChunkUnload(boolean canUnload) {
        if (!loaded) {
            return; //already unloaded
        }
        this.canUnload = canUnload;
    }

    @Nullable
    public World getWorld() {
        return world;
    }

    public Location getLocation() {
        return chunkPos;
    }

    public long getLastViewedTick() {
        return lastViewedTick;
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

    @Override
    public void dispose() {
        fbo.dispose();
    }
}
