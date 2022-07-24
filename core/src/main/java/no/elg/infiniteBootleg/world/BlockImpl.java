package no.elg.infiniteBootleg.world;

import static no.elg.infiniteBootleg.world.Material.AIR;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.util.CoordUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block in the world each block is a part of a chunk which is a part of a world. Each block know
 * its world location and its location within the parent chunk.
 *
 * @author Elg
 */
public class BlockImpl implements Block {

  private final Material material;
  private final World world;
  private final Chunk chunk;

  private final int localX;
  private final int localY;
  private boolean disposed;

  private boolean isLit;
  private boolean isSkylight;
  private final float[][] lightMap = new float[Block.LIGHT_RESOLUTION][Block.LIGHT_RESOLUTION];
  private final float[][] tmpLightMap = new float[Block.LIGHT_RESOLUTION][Block.LIGHT_RESOLUTION];

  public BlockImpl(
      @NotNull World world,
      @NotNull Chunk chunk,
      int localX,
      int localY,
      @NotNull Material material) {
    this.localX = localX;
    this.localY = localY;

    this.material = material;
    this.world = world;
    this.chunk = chunk;
  }

  @Override
  @Nullable
  public TextureRegion getTexture() {
    return getMaterial().getTextureRegion();
  }

  @Override
  @NotNull
  public Material getMaterial() {
    return material;
  }

  @Override
  @NotNull
  public Chunk getChunk() {
    return chunk;
  }

  @Override
  public World getWorld() {
    return world;
  }

  @Override
  public int getLocalX() {
    return localX;
  }

  @Override
  public int getLocalY() {
    return localY;
  }

  @Override
  public Location getLocation() {
    return new Location(getWorldX(), getLocalY());
  }

  @Override
  @Nullable
  public Block getRelative(@NotNull Direction dir) {
    return world.getRawBlock(getWorldX() + dir.dx, getWorldY() + dir.dy);
  }

  @Override
  public int getWorldX() {
    return chunk.getWorldX(localX);
  }

  @Override
  public int getWorldY() {
    return chunk.getWorldY(localY);
  }

  @Override
  @Nullable
  public Block getRawRelative(@NotNull Direction dir) {
    int newWorldX = getWorldX() + dir.dx;
    int newWorldY = getWorldY() + dir.dy;
    if (CoordUtil.worldToChunk(newWorldX) == chunk.getChunkX()
        && //
        CoordUtil.worldToChunk(newWorldY) == chunk.getChunkY()) {
      return chunk.getBlocks()[localX + dir.dx][localY + dir.dy];
    }
    return world.getRawBlock(newWorldX, newWorldY);
  }

  @Override
  public Block setBlock(@NotNull Material material) {
    return setBlock(material, true);
  }

  @Override
  public Block setBlock(@NotNull Material material, boolean update) {
    return chunk.setBlock(localX, localY, material, update);
  }

  @Override
  public void destroy(boolean updateTexture) {
    chunk.setBlock(localX, localY, (Block) null, updateTexture);
  }

  @Override
  public float[][] getLights() {
    synchronized (lightMap) {
      return lightMap;
    }
  }

  private void fillArray(float[][] mat, float newValue) {
    for (float[] arr : mat) {
      Arrays.fill(arr, newValue);
    }
  }

  @Override
  public void recalculateLighting() {
    //    System.out.println("Recalculating light for " + getMaterial() + " block " + getWorldX() +
    // "," + getWorldY());
    if (!Settings.renderLight) {
      return;
    }
    var chunkColumn = chunk.getChunkColumn();
    if (getMaterial() == AIR && chunkColumn.isBlockSkylight(localX, getWorldY())) {
      // This block is a skylight, its always lit fully
      isLit = true;
      isSkylight = true;
      synchronized (lightMap) {
        fillArray(lightMap, 1f);
      }
      return;
    }
    isLit = false;
    isSkylight = false;
    fillArray(tmpLightMap, 0f);

    // find light sources around this block

    int worldX = getWorldX();
    int worldY = getWorldY();

    Array<@NotNull Block> blocksAABB =
        chunk
            .getWorld()
            .getBlocksAABB(
                worldX + 0.5f,
                worldY + 0.5f,
                (float) Block.LIGHT_SOURCE_LOOK_BLOCKS,
                (float) Block.LIGHT_SOURCE_LOOK_BLOCKS,
                false,
                false);

    for (Block neighbor : blocksAABB) {

      var neighChunkCol = neighbor.getChunk().getChunkColumn();
      if (neighbor.getMaterial().isLuminescent()
          || (neighChunkCol.isBlockSkylight(neighbor.getLocalX(), neighbor.getWorldY())
              && neighbor.getMaterial() == AIR)) {

        isLit = true;
        for (int dx = 0; dx < Block.LIGHT_RESOLUTION; dx++) {
          for (int dy = 0; dy < Block.LIGHT_RESOLUTION; dy++) {
            // Calculate distance for each light cell
            var dist =
                (Location.distCubed(
                        worldX + ((float) dx / Block.LIGHT_RESOLUTION),
                        worldY + ((float) dy / Block.LIGHT_RESOLUTION),
                        neighbor.getWorldX() + 0.5,
                        neighbor.getWorldY() + 0.5))
                    / (Block.LIGHT_SOURCE_LOOK_BLOCKS * Block.LIGHT_SOURCE_LOOK_BLOCKS);
            var old = tmpLightMap[dx][dy];

            float normalizedIntensity;
            if (dist == 0.0) {
              normalizedIntensity = 0f;
            } else if (dist > 0) {
              normalizedIntensity = 1 - (float) (dist);
            } else {
              normalizedIntensity = 1 + (float) (dist);
            }

            if (old < normalizedIntensity) {
              tmpLightMap[dx][dy] = normalizedIntensity;
            }
          }
        }
      }
    }

    synchronized (lightMap) {
      for (int i = 0; i < Block.LIGHT_RESOLUTION; i++) {
        System.arraycopy(tmpLightMap[i], 0, lightMap[i], 0, Block.LIGHT_RESOLUTION);
      }
    }
  }

  @Override
  public boolean isLit() {
    return isLit;
  }

  @Override
  public boolean isSkylight() {
    return isSkylight;
  }

  @Override
  public ProtoWorld.Block.Builder save() {
    return Block.save(material);
  }

  @Override
  public void load(ProtoWorld.Block protoBlock) {
    Preconditions.checkArgument(protoBlock.getMaterialOrdinal() == material.ordinal());
  }

  @Override
  public void dispose() {
    if (disposed) {
      Main.logger()
          .warn(
              "Disposed block "
                  + getClass().getSimpleName()
                  + " ("
                  + getWorldX()
                  + ", "
                  + getWorldY()
                  + ") twice");
    }
    disposed = true;
  }

  @Override
  public boolean isDisposed() {
    return disposed;
  }

  @Override
  public int hashCode() {
    int result = material.hashCode();
    result = 31 * result + chunk.hashCode();
    result = 31 * result + localX;
    result = 31 * result + localY;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BlockImpl block = (BlockImpl) o;

    if (localX != block.localX) {
      return false;
    }
    if (localY != block.localY) {
      return false;
    }
    if (material != block.material) {
      return false;
    }
    return chunk.equals(block.chunk);
  }

  @Override
  public String toString() {
    return "Block{"
        + "material="
        + material
        + ", chunk="
        + chunk
        + ", worldX="
        + getWorldX()
        + ", worldY="
        + getWorldY()
        + '}';
  }
}
