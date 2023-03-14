package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.google.common.base.Preconditions;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.util.CoordUtilKt;
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
    return world.getRawBlock(getWorldX() + dir.dx, getWorldY() + dir.dy, true);
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
    if (CoordUtilKt.worldToChunk(newWorldX) == chunk.getChunkX()
        && //
        CoordUtilKt.worldToChunk(newWorldY) == chunk.getChunkY()) {
      return chunk.getBlocks()[localX + dir.dx][localY + dir.dy];
    }
    return world.getRawBlock(newWorldX, newWorldY, true);
  }

  @Override
  public Block setBlock(@NotNull Material material) {
    return setBlock(material, true);
  }

  @Override
  public Block setBlock(@NotNull Material material, boolean update) {
    return chunk.setBlock(localX, localY, material, update);
  }

  @NotNull
  public BlockLight getBlockLight() {
    return chunk.getBlockLight(localX, localY);
  }

  @Override
  public void destroy(boolean updateTexture) {
    Main.inst()
        .getScheduler()
        .executeAsync(() -> chunk.setBlock(localX, localY, (Block) null, updateTexture));
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
