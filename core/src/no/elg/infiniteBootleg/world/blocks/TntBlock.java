package no.elg.infiniteBootleg.world.blocks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.*;
import no.elg.infiniteBootleg.world.render.Updatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
        random = new Random(221);
    }

    private boolean white;
    private boolean exploded;
    private long tickStart;
    private static Random random;

    public static final long FUSE_DURATION = WorldTicker.TICKS_PER_SECOND * 2;
    public static final int EXPLOSION_RADIUS = 25;

    public TntBlock(@NotNull World world, Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
        tickStart = getWorld().getTick();
    }


    @Override
    public void update() {
        if (!exploded && getWorld().getTick() - tickStart > FUSE_DURATION) {
            exploded = true;
            Main.SCHEDULER.executeAsync(() -> {
                System.out.println("starting async");
                List<Location> destroy = new ArrayList<>();
                Location loc = getWorldLoc();
                for (int x = loc.x - EXPLOSION_RADIUS; x < loc.x + EXPLOSION_RADIUS; x++) {
                    for (int y = loc.y - EXPLOSION_RADIUS; y < loc.y + EXPLOSION_RADIUS; y++) {
                        Block b = getWorld().getBlock(x, y);
                        float hardness = b.getMaterial().getHardness();
                        if (b.getMaterial() == Material.AIR || hardness == 0) {
                            continue;
                        }
                        double dist = (loc.distCubed(b.getWorldLoc()) * hardness) * Math.abs(random.nextGaussian() + 10);
                        if (dist < EXPLOSION_RADIUS * EXPLOSION_RADIUS) {
                            destroy.add(b.getWorldLoc());
                        }
                    }
                }
                System.out.println("done calc async");

                Gdx.app.postRunnable(() -> {
                    Set<Chunk> chunks = ConcurrentHashMap.newKeySet();
                    for (Location location : destroy) {
                        chunks.add(getWorld().setBlock(location, null, false));
                    }
                    for (Chunk chunk : chunks) {
                        chunk.updateTexture(false);
                    }
                });


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
