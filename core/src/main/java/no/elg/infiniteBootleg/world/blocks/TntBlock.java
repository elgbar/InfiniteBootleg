package no.elg.infiniteBootleg.world.blocks;

import box2dLight.PointLight;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.util.PointLightPool;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import static no.elg.infiniteBootleg.world.Material.AIR;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block that explodes after {@link #fuseDuration} ticks
 *
 * @author Elg
 */
public class TntBlock extends TickingBlock {


    /**
     * Maximum explosion radius
     */
    public static final int EXPLOSION_STRENGTH = 40;
    /**
     * Randomness to what blocks are destroyed.
     * <p>
     * Lower means more blocks destroyed, but more random holes around the crater.
     * <p>
     * Higher means fewer blocks destroyed but less unconnected destroyed blocks. Note that too large will not look good
     * <p>
     * Minimum value should be above 3 as otherwise the edge of the explosion will clearly be visible
     */
    public final static int RESISTANCE = 8;
    private static final TextureRegion whiteTexture;

    static {
        if (Settings.renderGraphic) {
            Pixmap pixmap = new Pixmap(Block.BLOCK_SIZE, Block.BLOCK_SIZE, Pixmap.Format.RGBA4444);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            whiteTexture = new TextureRegion(new Texture(pixmap));
        }
        else {
            whiteTexture = null;
        }
    }

    /**
     * How long, in ticks, the fuse time should be
     */
    public final float fuseDuration;
    private final float strength;
    private boolean glowing;
    private boolean exploded;
    private long startTick;
    @Nullable
    private PointLight light;

    public TntBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
        strength = EXPLOSION_STRENGTH;
        fuseDuration = getWorld().getWorldTicker().getTPS() * 3f;
    }

    @Override
    public boolean shouldTick() {
        return !exploded;
    }

    @Override
    public void tick() {
        if (exploded) {
            return;
        }
        long currTick = getWorld().getTick();
        if (startTick == 0) {
            startTick = currTick;
        }
        long ticked = currTick - startTick;
        if (ticked > fuseDuration) {
            exploded = true;
            Main.inst().getScheduler().executeAsync(() -> {
                List<Block> destroyed = new ArrayList<>();
                int worldX = getWorldX();
                int worldY = getWorldY();
                for (int x = MathUtils.floor(worldX - strength); x < worldX + strength; x++) {
                    for (int y = MathUtils.floor(worldY - strength); y < worldY + strength; y++) {
                        synchronized (WorldRender.BOX2D_LOCK) {
                            Block b = getWorld().getBlock(x, y, true);
                            Material mat = b == null ? AIR : b.getMaterial();
                            float hardness = mat.getHardness();
                            if (mat == AIR || hardness < 0) {
                                continue;
                            }
                            double dist = Location.distCubed(worldX, worldY, b.getWorldX(), b.getWorldY()) * hardness *
                                          Math.abs(MathUtils.random.nextGaussian() + RESISTANCE);
                            if (dist < strength * strength) {
                                if (b instanceof TntBlock && b != this) {
                                    TntBlock tntb = (TntBlock) b;
                                    tntb.exploded = true;
                                }
                                destroyed.add(b);
                            }
                        }
                    }
                }

                Gdx.app.postRunnable(() -> {
                    Set<Chunk> chunks = new HashSet<>();
                    World world = getWorld();
                    for (Block block : destroyed) {
                        world.setBlock(block.getWorldX(), block.getWorldY(), (Block) null, false);
                        world.updateBlocksAround(block.getWorldX(), block.getWorldY());

                        chunks.add(block.getChunk());
                    }
                    for (Chunk chunk : chunks) {
                        chunk.updateTexture(false);
                    }
                });
            });
        }

        final long r = getWorld().getWorldTicker().getTPS() / 5;
        boolean old = glowing;
        if (ticked >= fuseDuration - getWorld().getWorldTicker().getTPS()) {
            glowing = true;
        }
        else if (ticked % r == 0) {
            glowing = !glowing;
        }

        if (old != glowing && Settings.renderGraphic) {
            if (light == null) {
                light = PointLightPool.inst.obtain();
                light.setPosition(getWorldX() + 0.5f, getWorldY() + 0.5f);
                light.setColor(Color.RED);
                light.setXray(true);
                light.setSoft(false);
                light.setDistance(16);
            }
            light.setActive(glowing);
            getChunk().updateTexture(true);
        }
    }

    @Override
    public @Nullable TextureRegion getTexture() {
        if (glowing) {
            return whiteTexture;
        }
        return super.getTexture();
    }

    @Override
    public void dispose() {
        if (light != null) {
            PointLightPool.inst.free(light);
        }
    }
}
