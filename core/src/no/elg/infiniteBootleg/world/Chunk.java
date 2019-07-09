package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.utils.Disposable;
import com.google.common.base.Preconditions;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.Binembly;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.Tuple;
import no.elg.infiniteBootleg.world.blocks.UpdatableBlock;
import no.elg.infiniteBootleg.world.render.Updatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.*;
import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Material.AIR;

/**
 * A piece of the world
 *
 * @author Elg
 */
public class Chunk implements Iterable<Block>, Updatable, Disposable, Binembly {

    public static final int CHUNK_SIZE = 32;
    public final static int CHUNK_TEXTURE_SIZE = CHUNK_SIZE * BLOCK_SIZE;
    public static final int CHUNK_SIZE_SHIFT = (int) (Math.log(CHUNK_SIZE) / Math.log(2));
    public static final String CHUNK_FOLDER = "chunks";
    public static final long CHUNK_UNLOAD_TIME = WorldTicker.TICKS_PER_SECOND * 5;

    private final World world;
    private final Block[][] blocks;

    private final int chunkX;
    private final int chunkY;

    private final Set<UpdatableBlock> updatableBlocks;

    private boolean modified; //if the chunk has been modified since loaded
    private boolean dirty; //if texture/allair needs to be updated
    private boolean prioritize; //if this chunk should be prioritized to be updated
    private boolean loaded; //once unloaded it no longer is valid
    private boolean allowUnload;
    private boolean initializing;

    private long lastViewedTick;
    private boolean allAir;
    private FrameBuffer fbo;
    private TextureRegion fboRegion;
    private FileHandle chunkFile;
    private Body box2dBody;

    private final static Tuple<Direction, byte[]>[] ts;

    static {
        //represent the direction to look and if no solid block there how to create a fixture at that location (ie
        // two relative vectors)
        // the value of the tuple is as follows dxStart, dyStart, dxEnd, dyEnd
        // this can be visually represented with a cube:
        //
        // (0,1)---(1,1)
        //   |       |
        //   |       |
        //   |       |
        // (0,0)---(1,0)
        //
        // where 'd' stands for delta
        // x/y is if this is the x or component of the coordinate
        // end/start is if this is the start or end vector
        //noinspection unchecked
        ts = new Tuple[4];
        ts[0] = new Tuple<>(Direction.NORTH, new byte[] {0, 1, 1, 1});
        ts[1] = new Tuple<>(Direction.EAST, new byte[] {1, 0, 1, 1});
        ts[2] = new Tuple<>(Direction.SOUTH, new byte[] {0, 0, 1, 0});
        ts[3] = new Tuple<>(Direction.WEST, new byte[] {0, 0, 0, 1});
    }

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


        updatableBlocks = Collections.synchronizedSet(new HashSet<>());

        allAir = false;
        loaded = true;
        allowUnload = true;

        dirty = true;
        prioritize = false;
        modified = false;
        initializing = true;
    }

    /**
     * Force update of texture and recalculate internal variables
     * This is usually called when the dirty flag of the chunk is set and either {@link #isAllAir()} or {@link
     * #getTextureRegion()}
     * called.
     */
    public void updateTextureNow() {
        if (initializing) { return; }
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
        if (Main.renderGraphic) {
            world.getRender().getChunkRenderer().queueRendering(this, prioritize);
            prioritize = false;
        }
    }

    public void updateFixture(boolean recalculateNeighbors) {
        if (box2dBody != null) {
            Body cpy = box2dBody;
            box2dBody = null;
            getWorld().getRender().getBox2dWorld().destroyBody(cpy);

        }
        if (allAir) {
            return;
        }

        //recalculate the shape of the chunk (box2d)

        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(chunkX * CHUNK_SIZE, chunkY * CHUNK_SIZE);
        box2dBody = getWorld().getRender().getBox2dWorld().createBody(bodyDef);
        box2dBody.setAwake(false);
        box2dBody.setFixedRotation(true);

        EdgeShape edgeShape = new EdgeShape();


        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localY = 0; localY < CHUNK_SIZE; localY++) {
                Block b = blocks[localX][localY];
                if (b == null || !b.getMaterial().blocksLight()) {
                    continue;
                }
                for (Tuple<Direction, byte[]> tuple : ts) {
                    Block rel = b.getRawRelative(tuple.key);
                    if (rel == null || !rel.getMaterial().blocksLight() || tuple.key == Direction.NORTH && localY == 0) {
                        byte[] ds = tuple.value;
                        edgeShape.set(localX + ds[0], localY + ds[1], localX + ds[2], localY + ds[3]);
                        box2dBody.createFixture(edgeShape, 0);
                    }
                }
            }
        }
        edgeShape.dispose();

        if (recalculateNeighbors) {
            //TODO Try to optimize this (ie select what directions to recalculate)
            for (Direction direction : Direction.CARDINAL) {
                Location relChunk = Location.relative(chunkX, chunkY, direction);
                if (getWorld().isChunkLoaded(relChunk)) {
                    getWorld().getChunk(relChunk).updateFixture(false);
                }
            }
        }
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
        if (blocks[localX][localY] == null) {
            setBlock(localX, localY, AIR, false);
        }
        return blocks[localX][localY];
    }

    /**
     * @param localX
     *     The local x ie a value between 0 and {@link #CHUNK_SIZE}
     * @param localY
     *     The local y ie a value between 0 and {@link #CHUNK_SIZE}
     * @param material
     *     The material to place, if {@code null} it will effectively be {@link Material#AIR}
     */
    public void setBlock(int localX, int localY, @Nullable Material material) {
        setBlock(localX, localY, material, true);
    }

    /**
     * @param localX
     *     The local x ie a value between 0 and {@link #CHUNK_SIZE}
     * @param localY
     *     The local y ie a value between 0 and {@link #CHUNK_SIZE}
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

        if (currBlock != null) {
            currBlock.dispose();
        }

        if (material == null) {
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
                if (update) {
                    ((UpdatableBlock) newBlock).update();
                }
            }
        }

        if (update) {
            modified = true;
            dirty = true;
            prioritize = true;
            getWorld().updateBlocksAround(getWorldX(localX), getWorldY(localY));
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
        modified = true;
        this.prioritize = prioritize;
    }

    /**
     * Might cause a call to {@link #updateTextureNow()} if the chunk is marked as dirty
     *
     * @return The texture of this chunk
     */
    @Nullable
    public TextureRegion getTextureRegion() {
        lastViewedTick = world.getTick();
        if (dirty) {
            updateTextureNow();
        }
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

    /**
     * Update all updatable blocks in this chunk
     */
    @Override
    public void update() {
        synchronized (updatableBlocks) {
            for (UpdatableBlock block : updatableBlocks) {
                block.tryUpdate();
            }
        }
    }

    /**
     * @return The backing array of the chunk, might contain null elements
     */
    public Block[][] getBlocks() {
        return blocks;
    }

    /**
     * Might cause a call to {@link #updateTextureNow()} if the chunk is marked as dirty
     *
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
     * Mark this chunk as unloaded, it will no longer be able to be updated.
     * <p>
     * <b>Note:</b> Internal use only, use {@link World#unload(Chunk)} to unload a chunk
     *
     * @return If the chunk was unloaded
     */
    boolean unload() {
        if (!allowUnload || !loaded) { return false;}
        loaded = false;
        allowUnload = false;
        if (box2dBody != null) {
            getWorld().getRender().getBox2dWorld().destroyBody(box2dBody);
            box2dBody = null;
        }

        for (Block[] blocks : blocks) {
            for (Block block : blocks) {
                if (block != null) {
                    block.dispose();
                }
            }
        }


        return true;
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

    @NotNull
    @Override
    public Iterator<Block> iterator() {
        return new Iterator<Block>() {
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
                return getBlock(x++, y);
            }
        };
    }

    public Stream<Block> stream() {
        Spliterator<Block> spliterator =
            Spliterators.spliterator(iterator(), CHUNK_SIZE * CHUNK_SIZE, SIZED | DISTINCT | NONNULL | ORDERED);
        return StreamSupport.stream(spliterator, false);
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
            chunkFile = getChunkFile(world, chunkX, chunkY);
        }
        return chunkFile;
    }

    @Nullable
    public static FileHandle getChunkFile(@NotNull World world, int chunkX, int chunkY) {
        FileHandle worldFile = world.worldFolder();
        if (worldFile == null) { return null; }
        return worldFile.child(CHUNK_FOLDER + File.separator + chunkX + File.separator + chunkY);
    }

    @NotNull
    @Override
    public byte[] disassemble() {
        byte[] bytes = new byte[CHUNK_SIZE * CHUNK_SIZE];
        int index = 0;
        for (Block block : this) {
            bytes[index++] = block == null ? 0 : block.disassemble()[0];
        }
        return bytes;
    }

    @Override
    public void assemble(@NotNull byte[] bytes) {
        Preconditions.checkArgument(bytes.length == CHUNK_SIZE * CHUNK_SIZE,
                                    "Invalid number of bytes. expected " + CHUNK_SIZE * CHUNK_SIZE + ", but got " + bytes.length);
        int index = 0;
        modified = true;
        for (int y = 0; y < CHUNK_SIZE; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Chunk chunk = (Chunk) o;

        if (chunkX != chunk.chunkX) { return false; }
        if (chunkY != chunk.chunkY) { return false; }
        if (loaded != chunk.loaded) { return false; }
        return world.equals(chunk.world);
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + chunkX;
        result = 31 * result + chunkY;
        return result;
    }

    @Override
    public String toString() {
        return "Chunk{" + "world=" + world + ", chunkX=" + chunkX + ", chunkY=" + chunkY + ", loaded=" + loaded + '}';
    }

    /**
     * Allow textures to be loaded
     */
    public void finishLoading() {
        initializing = false;
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                Block block = blocks[x][y];
                if (block instanceof UpdatableBlock) {
                    updatableBlocks.add((UpdatableBlock) block);
                }
            }
        }
    }
}
