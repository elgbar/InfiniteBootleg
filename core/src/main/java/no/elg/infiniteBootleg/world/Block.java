package no.elg.infiniteBootleg.world;

import static no.elg.infiniteBootleg.world.Material.AIR;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import no.elg.infiniteBootleg.CheckableDisposable;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.util.HUDDebuggable;
import no.elg.infiniteBootleg.util.Savable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Block
    extends CheckableDisposable, HUDDebuggable, Savable<ProtoWorld.Block.Builder> {

  int BLOCK_SIZE = 16;
  int LIGHT_RESOLUTION = 2;
  double LIGHT_SOURCE_LOOK_BLOCKS = 3.5;

  @Nullable
  static Block fromProto(
      @NotNull World world,
      @NotNull Chunk chunk,
      int localX,
      int localY,
      @Nullable ProtoWorld.Block protoBlock) {
    if (protoBlock == null) {
      return null;
    }
    Material mat = Material.fromOrdinal(protoBlock.getMaterialOrdinal());
    if (mat == AIR || mat.isEntity()) {
      return null;
    }
    final Block block = mat.createBlock(world, chunk, localX, localY);
    block.load(protoBlock);
    return block;
  }

  static ProtoWorld.Block.Builder save(Material material) {
    return ProtoWorld.Block.newBuilder().setMaterialOrdinal(material.ordinal());
  }

  @Nullable
  TextureRegion getTexture();

  @NotNull
  Material getMaterial();

  @NotNull
  Chunk getChunk();

  /**
   * @return World this block exists in
   */
  World getWorld();

  /**
   * @return The offset/local position of this block within its chunk
   */
  int getLocalX();

  /**
   * @return The offset/local position of this block within its chunk
   */
  int getLocalY();

  /**
   * @return {@code new Location(getWorldX(), getLocalY())}
   */
  Location getLocation();

  /**
   * @param dir The relative direction
   * @return The relative block in the given location
   * @see World#getBlock(int, int)
   */
  @Nullable
  Block getRelative(@NotNull Direction dir);

  /**
   * @return World location of this block
   */
  int getWorldX();

  /**
   * @return World location of this block
   */
  int getWorldY();

  /**
   * @param dir The relative direction
   * @return The relative raw block in the given location
   * @see World#getBlock(int, int)
   */
  @Nullable
  Block getRawRelative(@NotNull Direction dir);

  Block setBlock(@NotNull Material material);

  Block setBlock(@NotNull Material material, boolean update);

  @NotNull
  BlockLight getBlockLight();

  /**
   * Remove this block from the world
   *
   * @param updateTexture
   */
  void destroy(boolean updateTexture);

  void load(ProtoWorld.Block protoBlock);
}
