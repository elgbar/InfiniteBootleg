package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.google.common.base.Preconditions;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.Binembly;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.blocks.UpdatableBlock;
import no.elg.infiniteBootleg.world.render.Updatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
public class Chunk implements Iterable<Block>, Updatable, Disposable, Binembly {

    public static final int CHUNK_WIDTH = 32;
    public static final int CHUNK_HEIGHT = 32;

    private final World world;
    private final Location chunkPos;
    private final Block[][] blocks;

    private final Set<UpdatableBlock> updatableBlocks;

    private boolean modified; //if the chunk has been modified since loaded
    private boolean dirty; //if texture/allair needs to be updated
    private boolean prioritize; //if this chunk should be prioritized to be updated
    private boolean loaded; //once unloaded it no longer is valid
    private boolean canUnload;

    private long lastViewedTick;
    private boolean allAir;
    private FrameBuffer fbo;
    private TextureRegion fboRegion;
    private FileHandle chunkFile;

    /**
     * Create a new empty chunk
     *
     * @param world
     *     The world this chunk exists in
     * @param chunkPos
     *     The position of this chunk the given world
     */
    public Chunk(@NotNull World world, @NotNull Location chunkPos) {this(world, chunkPos, new Block[CHUNK_WIDTH][CHUNK_HEIGHT]);}

    /**
     * @param world
     *     The world this chunk exists in
     * @param chunkPos
     *     The position of this chunk the given world
     * @param blocks
     *     The initial blocks of this chunk (note: must be {@link #CHUNK_WIDTH}x{@link #CHUNK_HEIGHT})
     */
    public Chunk(@NotNull World world, @NotNull Location chunkPos, @NotNull Block[][] blocks) {
        Preconditions.checkArgument(blocks.length == CHUNK_WIDTH);
        Preconditions.checkArgument(blocks[0].length == CHUNK_HEIGHT);
        this.world = world;
        this.chunkPos = chunkPos;
        this.blocks = blocks;

        updatableBlocks = new HashSet<>();

        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                Block block = blocks[x][y];

                if (block instanceof UpdatableBlock) {
                    updatableBlocks.add((UpdatableBlock) block);
                }
            }
        }

        allAir = true;
        loaded = true;

        dirty = true;
        prioritize = false;
        modified = false;
    }

    /**
     * Force update of texture and recalculate internal variables
     * This is usually called when the dirty flag of the chunk is set and either {@link #isAllAir()} or {@link #getTexture()}
     * called.
     */
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
            setBlock(localX, localY, AIR, false);
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
     */
    public void setBlock(int localX, int localY, @Nullable Material material) {
        setBlock(localX, localY, material, true);
    }

    /**
     * @param localX
     *     The local x ie a value between 0 and {@link #CHUNK_WIDTH}
     * @param localY
     *     The local y ie a value between 0 and {@link #CHUNK_HEIGHT}
     * @param material
     *     The material to place, if {@code null} it will effectively be {@link Material#AIR}
     * @param update
     *     If the texture of this chunk should be updated
     */
    public void setBlock(int localX, int localY, @Nullable Material material, boolean update) {
        Preconditions.checkState(loaded, "Chunk is not loaded");

        Block currBlock = blocks[localX][localY];

        if ((currBlock == null && material == null) || (currBlock != null && currBlock.getMaterial() == material)) {
            return;
        }
        else if (material == null) {
            blocks[localX][localY] = null;
            if (currBlock instanceof UpdatableBlock) {
                updatableBlocks.remove(currBlock);
            }
        }
        else {
            Block newBlock = material.create(world, this, localX, localY);
            blocks[localX][localY] = newBlock;

            if (currBlock instanceof UpdatableBlock && !(newBlock instanceof UpdatableBlock)) {
                updatableBlocks.remove(currBlock);
            }
            if (newBlock instanceof UpdatableBlock) {
                updatableBlocks.add((UpdatableBlock) newBlock);
                ((UpdatableBlock) newBlock).update();
            }
        }

        modified = true;
        if (update) {
            dirty = true;
            prioritize = true;
            if (getWorld() != null) {
                getWorld().updateAround(getWorldLoc(localX, localY));
            }
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

    @Nullable
    public TextureRegion getTexture() {
        lastViewedTick = world.getTick();
        if (dirty) {
            updateTextureNow();
        }
        if (fbo == null) { return null; }
        return fboRegion;
    }

    /**
     * Update the framebuffer object of this chunk
     *
     * @param fbo
     *     The new fbo
     */
    public void setFbo(@NotNull FrameBuffer fbo) {
        if (this.fbo != null) {
            this.fbo.dispose();
        }
        this.fbo = fbo;
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);
    }

    @Override
    public void update() {
        for (UpdatableBlock block : updatableBlocks) {
            block.tryUpdate();
        }
    }

    @NotNull
    public Block[][] getBlocks() {
        return blocks;
    }

    /**
     * @return If all blocks in this chunk is air
     */
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
        if (!canUnload || !loaded) { return false;}
        loaded = false;
        canUnload = false;
        return true;
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

    /**
     * @return Location of this chunk in chunk coordinates
     */
    @NotNull
    public Location getLocation() {
        return chunkPos;
    }

    /**
     * This is the same as doing {@code CoordUtil.chunkToWorld(getLocation())}
     *
     * @return Location of this chunk in world coordinates
     */
    @NotNull
    public Location getWorldLoc() {
        return CoordUtil.chunkToWorld(chunkPos);
    }

    /**
     * @param localX
     *     The local chunk x coordinate
     * @param localY
     *     The local chunk y coordinate
     *
     * @return The world coordinate from the local position
     */
    @NotNull
    public Location getWorldLoc(int localX, int localY) {
        return new Location(CoordUtil.chunkToWorld(chunkPos.x) + localX, CoordUtil.chunkToWorld(chunkPos.y) + localY);
    }

    /**
     * @return The last tick this chunk's texture was pulled
     */
    public long getLastViewedTick() {
        return lastViewedTick;
    }

    /**
     * @return If the chunk has been modified since creation
     */
    public boolean isModified() {
        return modified;
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
        return 31 * world.hashCode() + chunkPos.hashCode();
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
                return getBlock(x++, y);
            }
        };
    }

    public Stream<Block> stream() {
        Spliterator<Block> spliterator =
            Spliterators.spliterator(iterator(), CHUNK_WIDTH * CHUNK_HEIGHT, SIZED | DISTINCT | NONNULL | ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    @Override
    public String toString() {
        return "Chunk{" + "world=" + world + ", chunkPos=" + chunkPos + '}';
    }

    @Override
    public void dispose() {
        if (fbo != null) {
            fbo.dispose();
        }
    }

    @Nullable
    public FileHandle getChunkFile() {
        if (chunkFile == null) {
            chunkFile = geChunkFile(world, chunkPos);
        }
        return chunkFile;
    }

    @Nullable
    public static FileHandle geChunkFile(@NotNull World world, @NotNull Location chunkPos) {
        FileHandle worldFile = world.worldFolder();
        if (worldFile == null) { return null; }
        return worldFile.child(World.CHUNK_FOLDER + File.separator + chunkPos.x + File.separator + chunkPos.y);
    }

    @NotNull
    @Override
    public byte[] disassemble() {
        byte[] bytes = new byte[CHUNK_WIDTH * CHUNK_HEIGHT];
        int index = 0;
        for (Block block : this) {
            bytes[index++] = block == null ? 0 : block.disassemble()[0];
        }
        return bytes;
    }

    @Override
    public void assemble(@NotNull byte[] bytes) {
        Preconditions.checkArgument(bytes.length == CHUNK_WIDTH * CHUNK_HEIGHT, "Invalid number of bytes");
        int index = 0;
        modified = true;
        for (int y = 0; y < CHUNK_HEIGHT; y++) {
            for (int x = 0; x < CHUNK_WIDTH; x++) {
                Material mat = Material.fromByte(bytes[index++]);
                if (mat == null || mat == AIR) {
                    blocks[x][y] = null;
                    continue;
                }
                Block block = mat.create(world, this, x, y);
                if (block instanceof UpdatableBlock) {
                    updatableBlocks.add((UpdatableBlock) block);
                }
                blocks[x][y] = block;
            }
        }
    }

}
