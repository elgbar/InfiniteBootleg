package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.Texture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
    public abstract Texture getTexture();

    @NotNull
    public abstract Material getMaterial();

    @Nullable
    public Chunk getChunk() {
        if (world == null) { return null; }
        return world.getChunk(loc);
    }

    public World getWorld() {
        return world;
    }

    public Location getLocation() {
        return loc;
    }
}
