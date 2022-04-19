package no.elg.infiniteBootleg.world.blocks;

import static no.elg.infiniteBootleg.world.Material.AIR;
import static no.elg.infiniteBootleg.world.render.WorldRender.LIGHT_LOCK;

import box2dLight.PointLight;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block that explodes after {@link #fuseDurationTicks} ticks
 *
 * @author Elg
 */
public class TntBlock extends LightBlock {

  /** Maximum explosion radius */
  public static final int EXPLOSION_STRENGTH = 40;
  /**
   * Randomness to what blocks are destroyed.
   *
   * <p>Lower means more blocks destroyed, but more random holes around the crater.
   *
   * <p>Higher means fewer blocks destroyed but less unconnected destroyed blocks. Note that too
   * large will not look good
   *
   * <p>Minimum value should be above 3 as otherwise the edge of the explosion will clearly be
   * visible
   */
  public static final int RESISTANCE = 8;

  public static final float FUSE_DURATION_SECONDS = 3f;
  public static final float ON_DISTANCE = 16f;
  public static final float OFF_DISTANCE = 0.1f;

  private static final TextureRegion whiteTexture;

  static {
    if (Settings.client) {
      Pixmap pixmap = new Pixmap(Block.BLOCK_SIZE, Block.BLOCK_SIZE, Pixmap.Format.RGBA4444);
      pixmap.setColor(Color.WHITE);
      pixmap.fill();
      whiteTexture = new TextureRegion(new Texture(pixmap));
    } else {
      whiteTexture = null;
    }
  }

  /** How long, in ticks, the fuse time should be */
  public final int fuseDurationTicks;

  private final float strength;
  private boolean glowing;
  private volatile boolean exploded;
  private long startTick;
  @Nullable private PointLight light;

  public TntBlock(
      @NotNull World world,
      @NotNull Chunk chunk,
      int localX,
      int localY,
      @NotNull Material material) {
    super(world, chunk, localX, localY, material);
    strength = EXPLOSION_STRENGTH;
    fuseDurationTicks = (int) (getWorld().getWorldTicker().getTPS() * FUSE_DURATION_SECONDS);
    startTick = getWorld().getTick();
  }

  @Override
  public boolean shouldTick() {
    return !exploded;
  }

  @Override
  public void customizeLight(@NotNull PointLight light) {
    light.setColor(Color.RED);
    light.setXray(false);
    light.setSoft(false);
    light.setStaticLight(true);
    light.setDistance(ON_DISTANCE);
  }

  @Override
  public void tick() {
    if (exploded) {
      return;
    }
    super.tick();

    long ticked = getWorld().getTick() - startTick;
    if (ticked > fuseDurationTicks && (Main.isAuthoritative())) {
      exploded = true;
      Main.inst()
          .getScheduler()
          .executeAsync(
              () -> {
                List<Block> destroyed = new ArrayList<>();
                int worldX = getWorldX();
                int worldY = getWorldY();
                World world = getWorld();
                for (int x = MathUtils.floor(worldX - strength); x < worldX + strength; x++) {
                  for (int y = MathUtils.floor(worldY - strength); y < worldY + strength; y++) {
                    Block b = world.getBlock(x, y, true);
                    Material mat = b == null ? AIR : b.getMaterial();
                    float hardness = mat.getHardness();
                    if (mat == AIR || hardness < 0) {
                      continue;
                    }
                    double dist =
                        Location.distCubed(worldX, worldY, b.getWorldX(), b.getWorldY())
                            * hardness
                            * Math.abs(MathUtils.random.nextGaussian() + RESISTANCE);
                    if (dist < strength * strength) {
                      if (b instanceof TntBlock && b != this) {
                        continue;
                      }
                      destroyed.add(b);
                    }
                  }
                }

                Set<Chunk> chunks = new HashSet<>();
                for (Block block : destroyed) {
                  block.destroy(false);
                  world.updateBlocksAround(block.getWorldX(), block.getWorldY());

                  chunks.add(block.getChunk());
                }
                for (Chunk chunk : chunks) {
                  chunk.updateTexture(false);
                }
              });
    }

    final long r = getWorld().getWorldTicker().getTPS() / 5;
    boolean old = glowing;
    if (ticked >= fuseDurationTicks - getWorld().getWorldTicker().getTPS()) {
      glowing = true;
    } else if (ticked % r == 0) {
      glowing = !glowing;
    }

    if (old != glowing && Settings.client) {

      synchronized (LIGHT_LOCK) {
        if (light != null) {
          light.setDistance(glowing ? ON_DISTANCE : OFF_DISTANCE);
          light.update();
        }
      }
      getChunk().updateTexture(true);
    }
  }

  public synchronized long getTicksLeft() {
    long ticked = getWorld().getTick() - startTick;
    return fuseDurationTicks - ticked;
  }

  public synchronized void setTicksLeft(long ticksLeft) {
    // Do min 1 as 0 would set startTick to current tick
    startTick = getWorld().getTick() - (fuseDurationTicks - ticksLeft);
    Preconditions.checkState(ticksLeft == getTicksLeft(), ticksLeft + " =/= " + getTicksLeft());
  }

  @Override
  public @Nullable TextureRegion getTexture() {
    if (glowing) {
      return whiteTexture;
    }
    return super.getTexture();
  }

  @Override
  public ProtoWorld.Block.Builder save() {
    return super.save().setTnt(ProtoWorld.Block.TNT.newBuilder().setTicksLeft(getTicksLeft()));
  }

  @Override
  public void load(ProtoWorld.Block protoBlock) {
    super.load(protoBlock);
    Preconditions.checkArgument(protoBlock.hasTnt());
    var tnt = protoBlock.getTnt();
    setTicksLeft(tnt.getTicksLeft());
  }

  @Override
  public @NotNull String hudDebug() {
    return "ticks left: " + getTicksLeft();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TntBlock)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    TntBlock block = (TntBlock) o;

    if (fuseDurationTicks != block.fuseDurationTicks) {
      return false;
    }
    if (Float.compare(block.strength, strength) != 0) {
      return false;
    }
    if (exploded != block.exploded) {
      return false;
    }
    return getTicksLeft()
        == block.getTicksLeft(); // FIXME what if tickID is different between the two calls?
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + fuseDurationTicks;
    result = 31 * result + (strength != +0.0f ? Float.floatToIntBits(strength) : 0);
    result = 31 * result + (exploded ? 1 : 0);
    return result;
  }

  @Override
  public void dispose() {
    super.dispose();
  }
}
