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
    private final Location chunkPos;
    private final Block[][] blocks;

    private final Set<UpdatableBlock> updatableBlocks;

    private boolean modified; //if the chunk has been modified since loaded
    private boolean dirty; //if texture/allair needs to be updated
    private boolean prioritize; //if this chunk should be prioritized to be updated
    private boolean loaded; //once unloaded it no longer is valid
    private boolean allowUnload;

    private long lastViewedTick;
    private boolean allAir;
    private FrameBuffer fbo;
    private TextureRegion fboRegion;
    private FileHandle chunkFile;
    private Body box2dBody;

    /**
     * Create a new empty chunk
     *
     * @param world
     *     The world this chunk exists in
     * @param chunkPos
     *     The position of this chunk the given world
     */
    public Chunk(@NotNull World world, @NotNull Location chunkPos) {
        this(world, chunkPos, new Block[CHUNK_SIZE][CHUNK_SIZE]);
    }

    /**
     * @param world
     *     The world this chunk exists in
     * @param chunkPos
     *     The position of this chunk the given world
     * @param blocks
     *     The initial blocks of this chunk (note: must be {@link #CHUNK_SIZE}x{@link #CHUNK_SIZE})
     */
    public Chunk(@NotNull World world, @NotNull Location chunkPos, @NotNull Block[][] blocks) {
        Preconditions.checkArgument(blocks.length == CHUNK_SIZE);
        Preconditions.checkArgument(blocks[0].length == CHUNK_SIZE);
        this.world = world;
        this.chunkPos = chunkPos;
        this.blocks = blocks;

        updatableBlocks = new HashSet<>();

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                Block block = blocks[x][y];
                if (block instanceof UpdatableBlock) {
                    updatableBlocks.add((UpdatableBlock) block);
                }
            }
        }

        allAir = true;
        loaded = true;
        allowUnload = true;

        dirty = true;
        prioritize = false;
        modified = false;
    }

    /**
     * Force update of texture and recalculate internal variables
     * This is usually called when the dirty flag of the chunk is set and either {@link #isAllAir()} or {@link
     * #getTextureRegion()}
     * called.
     */
    public void updateTextureNow() {
        dirty = false;
        //test if all the blocks in this chunk has the material air
        allAir = stream().allMatch(block -> block.getMaterial() == AIR);
//        allBlockLight = stream().allMatch(block -> block.getMaterial().blocksLight());
        if (Main.renderGraphic) {
            world.getRender().getChunkRenderer().queueRendering(this, prioritize);
            prioritize = false;

            if (box2dBody != null) {
                getWorld().getRender().getBox2dWorld().destroyBody(box2dBody);
                box2dBody = null;
            }
            if (allAir) {
                return;
            }

            //recalculate the shape of the chunk (box2d)


            BodyDef bodyDef = new BodyDef();
            bodyDef.position.set(chunkPos.x * CHUNK_SIZE, chunkPos.y * CHUNK_SIZE);
            box2dBody = getWorld().getRender().getBox2dWorld().createBody(bodyDef);
            box2dBody.setAwake(false);

            EdgeShape edgeShape = new EdgeShape();

            //TODO remove body if there is no air anywhere.
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_SIZE; y++) {
                    Block b = blocks[x][y];
                    if (b == null || !b.getMaterial().blocksLight()) {
                        continue;
                    }

                    int worldX = b.getLocalChunkLoc().x;
                    int worldY = b.getLocalChunkLoc().y;

                    //noinspection unchecked
                    Tuple<Direction, int[]>[] ts = new Tuple[4];
                    ts[0] = new Tuple<>(Direction.NORTH, new int[] {0, 1, 1, 1});
                    ts[1] = new Tuple<>(Direction.EAST, new int[] {1, 0, 1, 1});
                    ts[2] = new Tuple<>(Direction.SOUTH, new int[] {0, 0, 1, 0});
                    ts[3] = new Tuple<>(Direction.WEST, new int[] {0, 0, 0, 1});


                    for (Tuple<Direction, int[]> tuple : ts) {
                        Block rel = b.getRawRelative(tuple.key);
                        if (rel == null || !rel.getMaterial().blocksLight()) {
                            int[] ds = tuple.value;
                            edgeShape.set(worldX + ds[0], worldY + ds[1], worldX + ds[2], worldY + ds[3]);
                            box2dBody.createFixture(edgeShape, 0);
                        }
                    }
                }
            }
            edgeShape.dispose();
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
            getWorld().updateBlocksAround(getWorldLoc(localX, localY));
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

    @Override
    public void update() {
        for (UpdatableBlock block : updatableBlocks) {
            block.tryUpdate();
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
     *
     * @see CoordUtil#chunkToWorld(Location)
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
     * @return The world coordinate from the local position as offset
     *
     * @see CoordUtil#chunkToWorld(Location, int, int)
     */
    @NotNull
    public Location getWorldLoc(int localX, int localY) {
        return CoordUtil.chunkToWorld(chunkPos, localX, localY);
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
            chunkFile = getChunkFile(world, chunkPos);
        }
        return chunkFile;
    }

    @Nullable
    public static FileHandle getChunkFile(@NotNull World world, @NotNull Location chunkPos) {
        FileHandle worldFile = world.worldFolder();
        if (worldFile == null) { return null; }
        return worldFile.child(CHUNK_FOLDER + File.separator + chunkPos.x + File.separator + chunkPos.y);
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
}
