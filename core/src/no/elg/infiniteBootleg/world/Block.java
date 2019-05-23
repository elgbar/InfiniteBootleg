package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Elg
 */
public class Block {

    private final Material material;
    private Location worldLoc;
    private World world;
    private Location chunkLoc;

    public Block(@NotNull World world, Chunk chunk, int localX, int localY, @NotNull Material material) {

        Location worldChunkLoc = chunk.getWorldLoc();
        int worldX = worldChunkLoc.x + localX;
        int worldY = worldChunkLoc.y + localY;

        this.worldLoc = new Location(worldX, worldY);
        chunkLoc = new Location(localX, localY);

        this.world = world;
        this.material = material;
    }

    @Nullable
    public TextureRegion getTexture() {
        return getMaterial().getTexture();
    }

    @NotNull
    public Material getMaterial() {
        return material;
    }

    @NotNull
    public Chunk getChunk() {
        return world.getChunkFromWorld(worldLoc);
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

    public Location getChunkLoc() {
        return chunkLoc;
    }

    /**
     * @param dir
     *
     * @return The relative block in the given location
     */
    public Block getRelative(@NotNull Direction dir) {
        return world.getBlock(worldLoc.x + dir.dx, worldLoc.y + dir.dy);
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
        int result = worldLoc.hashCode();
        result = 31 * result + (world != null ? world.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getMaterial() + "-block{" + "loc=" + worldLoc + ", world=" + world + '}';
    }
}
