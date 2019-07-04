package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.util.Binembly;
import no.elg.infiniteBootleg.util.CoordUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
    private Location worldLoc;
    private Location localLoc;

    public Block(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        worldLoc = CoordUtil.chunkToWorld(chunk.getLocation(), localX, localY);
        localLoc = new Location(localX, localY);

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
    public Location getWorldLoc() {
        return worldLoc;
    }

    /**
     * @return The offset/local position of this block within its chunk
     */
    public Location getLocalLoc() {
        return localLoc;
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
        return world.getBlock(worldLoc.x + dir.dx, worldLoc.y + dir.dy);
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
        return world.getRawBlock(worldLoc.x + dir.dx, worldLoc.y + dir.dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Block block = (Block) o;

        if (!worldLoc.equals(block.worldLoc)) { return false; }
        return Objects.equals(world, block.world);
    }


    @Override
    public int hashCode() {
        int result = material.hashCode();
        result = 31 * result + worldLoc.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getMaterial() + "-block{" + "loc=" + worldLoc + ", world=" + world + '}';
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
}
