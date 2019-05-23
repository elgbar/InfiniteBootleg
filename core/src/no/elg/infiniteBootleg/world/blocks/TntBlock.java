package no.elg.infiniteBootleg.world.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.*;
import no.elg.infiniteBootleg.world.render.Updatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class TntBlock extends Block implements Updatable {


    private static final TextureRegion whiteTexture;

    static {
        if (Main.renderGraphic) {
            Pixmap pixmap = new Pixmap(World.BLOCK_SIZE, World.BLOCK_SIZE, Pixmap.Format.RGBA4444);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            whiteTexture = new TextureRegion(new Texture(pixmap));
        }
        else {
            whiteTexture = null;
        }
    }

    private boolean white;
    private long tickStart;

    public static final long FUSE_DURATION = WorldTicker.TICKS_PER_SECOND * 5;
    public static final int EXPLOSION_RADIUS = 11;

    public TntBlock(@NotNull World world, Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
        tickStart = getWorld().getTick();
    }


    @Override
    public void update() {
        if (getWorld().getTick() - tickStart > FUSE_DURATION) {
            Main.SCHEDULER.executeSync(() -> {
                Location loc = getWorldLoc();
                for (int x = loc.x - EXPLOSION_RADIUS; x < loc.x + EXPLOSION_RADIUS; x++) {
                    for (int y = loc.y - EXPLOSION_RADIUS; y < loc.y + EXPLOSION_RADIUS; y++) {
                        Block b = getWorld().getBlock(x, y);
                        double dist = loc.distCubed(b.getWorldLoc()) * b.getMaterial().getHardness();
                        if (dist < EXPLOSION_RADIUS * EXPLOSION_RADIUS) {
                            getWorld().setBlock(b.getWorldLoc(), null);
                        }
                    }
                }

            });
        }
        if (getWorld().getTick() % (WorldTicker.TICKS_PER_SECOND / 6) == 0) {
            white = !white;
            if (Main.renderGraphic) {
                getChunk().updateTexture(true);
            }
        }
    }

    @Override
    public @Nullable TextureRegion getTexture() {
        if (white) {
            return whiteTexture;
        }
        return super.getTexture();
    }
}
