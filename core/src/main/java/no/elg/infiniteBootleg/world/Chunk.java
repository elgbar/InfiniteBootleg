package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.google.common.base.Preconditions;
import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.util.Binembly;
import no.elg.infiniteBootleg.util.CoordUtil;
import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Material.AIR;
import no.elg.infiniteBootleg.world.blocks.TickingBlock;
import no.elg.infiniteBootleg.world.box2d.ChunkBody;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A piece of the world
 *
 * @author Elg
 */
public class Chunk implements Iterable<Block>, Ticking, Disposable, Binembly {

    public static final int CHUNK_SIZE = 32;
    public static final int CHUNK_TEXTURE_SIZE = CHUNK_SIZE * BLOCK_SIZE;
    public static final int CHUNK_SIZE_SHIFT = (int) (Math.log(CHUNK_SIZE) / Math.log(2));
    public static final String CHUNK_FOLDER = "chunks";

    private final World world;
    private final Block[][] blocks;

    private final int chunkX;
    private final int chunkY;

    /**
     * Must be accessed under a synchronized self block i.e {@code synchronized(tickingBlocks){...}}
     */
    private final Array<TickingBlock> tickingBlocks;
    private final ChunkBody chunkBody;
    //if this chunk should be prioritized to be updated
    private volatile boolean dirty; //if texture/allair needs to be updated
    private volatile boolean prioritize;
    private volatile boolean modified; //if the chunk has been modified since loaded
    private volatile boolean loaded; //once unloaded it no longer is valid
    private volatile boolean allowUnload;
    private volatile boolean initializing;
    private volatile boolean allAir;
    private volatile long lastViewedTick;
    private TextureRegion fboRegion;
    private FileHandle chunkFile;
    private FrameBuffer fbo;

    /**
     * Create a new empty chunk
     *
     * @param world
     *     The world this chunk exists in
     * @param chunkX
     *     The position x of this chunk the given world
     * @param chunkY
     *     The position y of this chunk the given world
     */
    public Chunk(@NotNull World world, int chunkX, int chunkY) {
        this(world, chunkX, chunkY, new Block[CHUNK_SIZE][CHUNK_SIZE]);
    }

    /**
     * @param world
     *     The world this chunk exists in
     * @param chunkX
     *     The position x of this chunk the given world
     * @param chunkY
     *     The position y of this chunk the given world
     * @param blocks
     *     The initial blocks of this chunk (note: must be {@link #CHUNK_SIZE}x{@link #CHUNK_SIZE})
     */
    public Chunk(@NotNull World world, int chunkX, int chunkY, @NotNull Block[][] blocks) {
        Preconditions.checkArgument(blocks.length == CHUNK_SIZE);
        Preconditions.checkArgument(blocks[0].length == CHUNK_SIZE);
        this.world = world;
        this.blocks = blocks;
        this.chunkX = chunkX;
        this.chunkY = chunkY;

        tickingBlocks = new Array<>(false, CHUNK_SIZE);
        chunkBody = new ChunkBody(this);

        dirty = true;
        prioritize = false;

        allAir = false;
        loaded = true;
        allowUnload = true;
        modified = false;
        initializing = true;

    }

    /**
     * Set a block and update all blocks around it
     *
     * @param localX
     *     The local x ie a value between 0 and {@link #CHUNK_SIZE}
     * @param localY
     *     The local y ie a value between 0 and {@link #CHUNK_SIZE}
     * @param material
     *     The material of the new block
     *
     * @return The new block, only {@code null} if {@code material} parameter is {@code null}
     */
    @Contract("_,_,!null->!null;_,_,null->null")
    public Block setBlock(int localX, int localY, @Nullable Material material) {
        return setBlock(localX, localY, material, true);
    }

    /**
     * @param localX
     *     The local x ie a value between 0 and {@link #CHUNK_SIZE}
     * @param localY
     *     The local y ie a value between 0 and {@link #CHUNK_SIZE}
     * @param material
     *     The material of the new block
     * @param update
     *     If the texture of this chunk should be updated
     *
     * @return The new block, only {@code null} if {@code material} parameter is {@code null}
     */
    @Contract("_,_,!null,_->!null;_,_,null,_->null")
    public Block setBlock(int localX, int localY, @Nullable Material material, boolean update) {
        Block block = material == null ? null : material.createBlock(world, this, localX, localY);
        return setBlock(localX, localY, block, update);
    }

    /**
     * @param localX
     *     The local x ie a value between 0 and {@link #CHUNK_SIZE}
     * @param localY
     *     The local y ie a value between 0 and {@link #CHUNK_SIZE}
     * @param block
     *     The new block
     * @param updateTexture
     *     If the texture of this chunk should be updated
     *
     * @return The given block, equal to the {@code block} parameter
     */
    @Contract("_,_,!null,_->!null;_,_,null,_->null")
    public Block setBlock(int localX, int localY, @Nullable Block block, boolean updateTexture) {
        Preconditions.checkState(loaded, "Chunk is not loaded");

        if (block != null) {
            Preconditions.checkArgument(block.getLocalX() == localX);
            Preconditions.checkArgument(block.getLocalY() == localY);
            Preconditions.checkArgument(block.getChunk() == this);
        }
        synchronized (this) {
            Block currBlock = blocks[localX][localY];

            //accounts for both being null also ofc
            if (currBlock == block) {
                return currBlock;
            }
            else if (currBlock != null && block != null && currBlock.getMaterial() == block.getMaterial()) {
                //Block is the same, ignore this set
                block.dispose();
                return currBlock;
            }

            if (currBlock != null) {
                currBlock.dispose();

                if (currBlock instanceof TickingBlock tickingBlock) {
                    tickingBlocks.removeValue(tickingBlock, true);
                }
            }

            blocks[localX][localY] = block;
            if (block instanceof TickingBlock tickingBlock) {
                tickingBlocks.add(tickingBlock);
            }

            if (updateTexture) {
                modified = true;
                dirty = true;
                prioritize = true;
            }
        }
        if (updateTexture) {
            //TODO maybe this can be done async? (it was before)
            world.updateBlocksAround(getWorldX(localX), getWorldY(localY));
        }
        return block;
    }

    /**
     * @param localX
     *     The local chunk x coordinate
     *
     * @return The world coordinate from the local position as offset
     *
     * @see CoordUtil#chunkToWorld(int, int)
     */
    public int getWorldX(int localX) {
        return CoordUtil.chunkToWorld(chunkX, localX);
    }

    /**
     * @param localY
     *     The local chunk y coordinate
     *
     * @return The world coordinate from the local position as offset
     *
     * @see CoordUtil#chunkToWorld(int, int)
     */
    public int getWorldY(int localY) {
        return CoordUtil.chunkToWorld(chunkY, localY);
    }

    /**
     * Force update of this chunk's texture and invariants
     *
     * @param prioritize
     *     If this chunk should be prioritized when rendering
     */
    public void updateTexture(boolean prioritize) {
        dirty = true;
        modified = true;
        this.prioritize = prioritize;
    }

    /**
     * Might cause a call to {@link #updateTextureIfDirty()} if the chunk is marked as dirty
     *
     * @return The texture of this chunk
     */
    @Nullable
    public TextureRegion getTextureRegion() {
        if (dirty) {
            updateTextureIfDirty();
        }
        return fboRegion;
    }

    /**
     * Force update of texture and recalculate internal variables
     * This is usually called when the dirty flag of the chunk is set and either {@link #isAllAir()} or {@link
     * #getTextureRegion()}
     * called.
     */
    public void updateTextureIfDirty() {
        if (initializing) {
            return;
        }
        synchronized (this) {
            if (!dirty) {
                return;
            }
            dirty = false;

            //test if all the blocks in this chunk has the material air
            allAir = true;
            outer:
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                for (int localY = 0; localY < CHUNK_SIZE; localY++) {
                    Block b = blocks[localX][localY];
                    if (b != null && b.getMaterial() != AIR) {
                        allAir = false;
                        break outer;
                    }
                }
            }

        }
        if (Settings.renderGraphic) {
            world.getRender().getChunkRenderer().queueRendering(this, prioritize);
            prioritize = false;
        }
    }

    public void view() {
        lastViewedTick = world.getTick();
    }

    public synchronized FrameBuffer getFbo() {
        if (fbo == null) {
            fbo = new FrameBuffer(Pixmap.Format.RGBA4444, CHUNK_TEXTURE_SIZE, CHUNK_TEXTURE_SIZE, false);
            fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            fboRegion = new TextureRegion(fbo.getColorBufferTexture());
            fboRegion.flip(false, true);
        }
        return fbo;
    }

    /**
     * Update all updatable blocks in this chunk
     */
    @Override
    public void tick() {
        Preconditions.checkState(loaded, "Chunk is not loaded");
        synchronized (this) {
            for (TickingBlock block : tickingBlocks) {
                block.tryTick(false);
            }
        }
    }

    @Override
    public void tickRare() {
        Preconditions.checkState(loaded, "Chunk is not loaded");
        synchronized (this) {
            for (TickingBlock block : tickingBlocks) {
                block.tryTick(true);
            }
        }
    }

    /**
     * @return The backing array of the chunk, might contain null elements
     */
    public Block[][] getBlocks() {
        return blocks;
    }

    @Nullable
    public Block getRawBlock(int localX, int localY) {
        return blocks[localX][localY];
    }

    /**
     * Might cause a call to {@link #updateTextureIfDirty()} if the chunk is marked as dirty
     *
     * @return If all blocks in this chunk is air
     */
    public boolean isAllAir() {
        if (dirty) {
            updateTextureIfDirty();
        }

        return allAir;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /**
     * If {@code isAllowingUnloading} is {@code false} this chunk cannot be unloaded
     *
     * @param allowUnload
     *     If the chunk can be unloaded or not
     */
    public void setAllowUnload(boolean allowUnload) {
        if (!loaded) {
            return; //already unloaded
        }
        this.allowUnload = allowUnload;
    }

    public boolean isAllowingUnloading() {
        return allowUnload;
    }

    @NotNull
    public World getWorld() {
        return world;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkY() {
        return chunkY;
    }

    /**
     * @return Location of this chunk in world coordinates
     *
     * @see CoordUtil#chunkToWorld(Location)
     */
    public int getWorldX() {
        return CoordUtil.chunkToWorld(chunkX);
    }

    /**
     * This is the same as doing {@code CoordUtil.chunkToWorld(getLocation())}
     *
     * @return Location of this chunk in world coordinates
     *
     * @see CoordUtil#chunkToWorld(Location)
     */
    public int getWorldY() {
        return CoordUtil.chunkToWorld(chunkY);
    }

    /**
     * f
     *
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

    public Stream<Block> stream() {
        Spliterator<Block> spliterator = Spliterators.spliterator(iterator(), (long) CHUNK_SIZE * CHUNK_SIZE, SIZED | DISTINCT | NONNULL | ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    @NotNull
    @Override
    public Iterator<Block> iterator() {
        return new Iterator<>() {
            int x;
            int y;

            @Override
            public boolean hasNext() {
                return y < CHUNK_SIZE - 1 || x < CHUNK_SIZE;
            }

            @Override
            public Block next() {
                if (x == CHUNK_SIZE) {
                    x = 0;
                    y++;
                }
                if (y >= CHUNK_SIZE) {
                    throw new NoSuchElementException();
                }
                return getBlock(x++, y);
            }
        };
    }

    /**
     * @param localX
     *     The local x ie a value between 0 and {@link #CHUNK_SIZE}
     * @param localY
     *     The local y ie a value between 0 and {@link #CHUNK_SIZE}
     *
     * @return A block from the relative coordinates
     */
    @NotNull
    public Block getBlock(int localX, int localY) {
        Preconditions.checkState(loaded, "Chunk is not loaded");
        Preconditions.checkArgument(CoordUtil.isInsideChunk(localX, localY),
                                    "Given arguments are not inside this chunk, localX=" + localX + " localY=" + localY);
        synchronized (this) {
            Block block = blocks[localX][localY];

            if (block == null) {
                return setBlock(localX, localY, AIR, false);
            }
            return block;
        }
    }

    @Override
    public synchronized void dispose() {
        loaded = false;
        allowUnload = false;

        if (fbo != null) {
            Main.inst().getScheduler().executeSync(fbo::dispose);
        }

        chunkBody.dispose();
        tickingBlocks.clear();

        for (Block[] blockArr : blocks) {
            for (Block block : blockArr) {
                if (block != null) {
                    block.dispose();
                }
            }
        }
    }

    @Nullable
    public FileHandle getChunkFile() {
        if (chunkFile == null) {
            chunkFile = getChunkFile(world, chunkX, chunkY);
        }
        return chunkFile;
    }

    @Nullable
    public static FileHandle getChunkFile(@NotNull World world, int chunkX, int chunkY) {
        FileHandle worldFile = world.worldFolder();
        if (worldFile == null) {
            return null;
        }
        return worldFile.child(CHUNK_FOLDER + File.separator + chunkX + File.separator + chunkY);
    }

    @NotNull
    public ChunkBody getChunkBody() {
        return chunkBody;
    }

    /**
     * @return {@code true} if all the {@link Direction#CARDINAL} neighbors are loaded
     */
    public boolean isNeighborsLoaded() {
        for (Direction direction : Direction.CARDINAL) {
            Location relChunk = Location.relative(chunkX, chunkY, direction);
            if (!world.isChunkLoaded(relChunk)) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    /**
     * Allow textures to be loaded. Only call once per chunk
     */
    public synchronized void finishLoading() {
        if (!initializing) {
            return;
        }
        initializing = false;

        tickingBlocks.clear();
        tickingBlocks.ensureCapacity(CHUNK_SIZE);
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                Block block = blocks[x][y];
                if (block instanceof TickingBlock tickingBlock) {
                    tickingBlocks.add(tickingBlock);
                }
            }
        }
    }

    @Override
    public byte[] disassemble() {
        byte[] bytes = new byte[CHUNK_SIZE * CHUNK_SIZE];
        int index = 0;
        synchronized (this) {
            for (Block block : this) {
                bytes[index++] = block == null ? 0 : block.disassemble()[0];
            }
        }
        return bytes;
    }

    @Override
    public void assemble(byte[] bytes) {
        Preconditions.checkArgument(bytes.length == CHUNK_SIZE * CHUNK_SIZE,
                                    "Invalid number of bytes. expected " + CHUNK_SIZE * CHUNK_SIZE + ", but got " + bytes.length);
        int index = 0;
        synchronized (this) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    Material mat = Material.fromByte(bytes[index++]);
                    if (mat == null || mat == AIR) {
                        continue;
                    }
                    blocks[x][y] = mat.createBlock(world, this, x, y);
                }
            }
        }
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + chunkX;
        result = 31 * result + chunkY;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Chunk chunk = (Chunk) o;

        if (chunkX != chunk.chunkX) {
            return false;
        }
        if (chunkY != chunk.chunkY) {
            return false;
        }
        return world.equals(chunk.world);
    }

    @Override
    public String toString() {
        return "Chunk{" + "world=" + world + ", chunkX=" + chunkX + ", chunkY=" + chunkY + ", loaded=" + loaded + '}';
    }
}
