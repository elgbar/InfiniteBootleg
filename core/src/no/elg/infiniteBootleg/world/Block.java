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
    private Location loc;
    private World world;

    public Block(int x, int y, @NotNull World world, @NotNull Material material) {
        this.loc = new Location(x, y);
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
        return world.getChunkFromWorld(loc);
    }

    public World getWorld() {
        return world;
    }

    public Location getLocation() {
        return loc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Block block = (Block) o;

        if (!loc.equals(block.loc)) { return false; }
        return Objects.equals(world, block.world);
    }

    @Override
    public int hashCode() {
        int result = loc.hashCode();
        result = 31 * result + (world != null ? world.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getMaterial() + "-block{" + "loc=" + loc + ", world=" + world + '}';
    }
}
