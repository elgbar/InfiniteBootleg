package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Elg
 */
public abstract class Block {

    private Location loc;
    private World world;

    public Block(int x, int y, @Nullable World world) {

        this.loc = new Location(x, y);
        this.world = world;
    }

    @Nullable
    public TextureRegion getTexture() {
        return getMaterial().getTexture();
    }

    @NotNull
    public abstract Material getMaterial();

    @Nullable
    public Chunk getChunk() {
        if (world == null) { return null; }
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
