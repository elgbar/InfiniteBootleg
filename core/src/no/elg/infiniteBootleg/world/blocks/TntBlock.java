package no.elg.infiniteBootleg.world.blocks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static no.elg.infiniteBootleg.world.Material.AIR;

/**
 * @author Elg
 */
public class TntBlock extends UpdatableBlock {


    private static final TextureRegion whiteTexture;

    static {
        if (Main.renderGraphic) {
            Pixmap pixmap = new Pixmap(Block.BLOCK_SIZE, Block.BLOCK_SIZE, Pixmap.Format.RGBA4444);
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
    private long tickLeft;
    private double strength;
    private static Random random;

    public static final long FUSE_DURATION = WorldTicker.TICKS_PER_SECOND * 2;
    public static final int EXPLOSION_STRENGTH = 25; //basically max radius
    public static final int RESISTANCE = 10;

    public TntBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
        tickLeft = FUSE_DURATION;
        strength = EXPLOSION_STRENGTH;
    }


    @Override
    public void update() {
        if (exploded) { return; }
        setUpdate(true); //continue to update this block till it explodes
        if (tickLeft <= 0) {
            exploded = true;
            Main.SCHEDULER.executeAsync(() -> {
                List<Block> destroyed = new ArrayList<>();

                for (int x = (int) Math.floor(worldX - strength); x < worldX + strength; x++) {
                    for (int y = (int) Math.floor(worldY - strength); y < worldY + strength; y++) {
                        Block b = getWorld().getRawBlock(x, y);
                        Material mat = b == null ? AIR : b.getMaterial();
                        float hardness = mat.getHardness();
                        if (mat == AIR || hardness < 0) {
                            continue;
                        }
                        double dist = Location.distCubed(worldX, worldY, b.getWorldX(), b.getWorldY()) * hardness *
                                      Math.abs(random.nextGaussian() + RESISTANCE);
                        if (dist < strength * strength) {
                            if (b instanceof TntBlock && b != this) {
                                TntBlock tntb = (TntBlock) b;
                                tntb.exploded = true;
                            }
                            destroyed.add(b);
                        }
                    }
                }

                Gdx.app.postRunnable(() -> {
                    Set<Chunk> chunks = new HashSet<>();
                    for (Block block : destroyed) {
                        getWorld().setBlock(block.getWorldX(), block.getWorldY(), null, false);
                        chunks.add(block.getChunk());
                        getWorld().updateBlocksAround(block.getWorldX(), block.getWorldY());
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
        tickLeft--;
    }

    @Override
    public @Nullable TextureRegion getTexture() {
        if (white) { return whiteTexture; }
        return super.getTexture();
    }
}
