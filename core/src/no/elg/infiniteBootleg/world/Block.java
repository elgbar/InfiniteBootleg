package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.util.Binembly;
import no.elg.infiniteBootleg.util.CoordUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block in the world each block is a part of a chunk which is a part of a world. Each block know its world location and its
 * location within the parent chunk.
 *
 * @author Elg
 */
public class Block implements Binembly, Disposable {

    public final static int BLOCK_SIZE = 16;

    private Material material;
    private World world;
    private Chunk chunk;

    protected int worldX;
    protected int worldY;

    private int localX;
    private int localY;

    public Block(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        this.localX = localX;
        this.localY = localY;
        worldX = chunk.getWorldX(localX);
        worldY = chunk.getWorldY(localY);

        this.material = material;
        this.world = world;
        this.chunk = chunk;
    }

    @Nullable
    public TextureRegion getTexture() {
        return getMaterial().getTextureRegion();
    }

    @NotNull
    public Material getMaterial() {
        return material;
    }

    @NotNull
    public Chunk getChunk() {
        return chunk;
    }

    /**
     * @return World this block exists in
     */
    public World getWorld() {
        return world;
    }


    /**
     * @return World location of this block
     */
    public int getWorldX() {
        return worldX;
    }

    /**
     * @return World location of this block
     */
    public int getWorldY() {
        return worldY;
    }

    /**
     * @return The offset/local position of this block within its chunk
     */
    public int getLocalX() {
        return localX;
    }

    /**
     * @return The offset/local position of this block within its chunk
     */
    public int getLocalY() {
        return localY;
    }

    /**
     * @param dir
     *     The relative direction
     *
     * @return The relative block in the given location
     *
     * @see World#getBlock(int, int)
     */
    @NotNull
    public Block getRelative(@NotNull Direction dir) {
        return world.getBlock(worldX + dir.dx, worldY + dir.dy);
    }

    /**
     * @param dir
     *     The relative direction
     *
     * @return The relative raw block in the given location
     *
     * @see World#getRawBlock(int, int)
     */
    @Nullable
    public Block getRawRelative(@NotNull Direction dir) {
        if (CoordUtil.worldToChunk(worldX + dir.dx) == chunk.getChunkX() && //
            CoordUtil.worldToChunk(worldY + dir.dy) == chunk.getChunkY()) {
            return chunk.getBlocks()[localX + dir.dx][localY + dir.dy];
        }
        return world.getRawBlock(worldX + dir.dx, worldY + dir.dy);
    }

    @NotNull
    @Override
    public byte[] disassemble() {
        return new byte[] {(byte) material.ordinal()};
    }

    @Override
    public void assemble(@NotNull byte[] bytes) {
        throw new UnsupportedOperationException("Cannot assemble blocks directly. Blocks must be assembled by a chunk");
    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Block block = (Block) o;

        if (worldX != block.worldX) { return false; }
        if (worldY != block.worldY) { return false; }
        if (material != block.material) { return false; }
        return world.equals(block.world);
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + worldX;
        result = 31 * result + worldY;
        return result;
    }

    @Override
    public String toString() {
        return "Block{" + "material=" + material + ", world=" + world + ", chunk=" + chunk + ", worldX=" + worldX + ", worldY=" +
               worldY + ", localX=" + localX + ", localY=" + localY + '}';
    }
}
