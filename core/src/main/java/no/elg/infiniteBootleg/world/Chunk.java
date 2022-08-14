package no.elg.infiniteBootleg.world;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Array;
import java.util.stream.Stream;
import no.elg.infiniteBootleg.CheckableDisposable;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.blocks.TickingBlock;
import no.elg.infiniteBootleg.world.box2d.ChunkBody;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A piece of the world
 *
 * @author Elg
 */
public interface Chunk
    extends Iterable<@Nullable Block>, Ticking, CheckableDisposable, Comparable<Chunk> {

  int CHUNK_SIZE = 32;
  int CHUNK_TEXTURE_SIZE = CHUNK_SIZE * BLOCK_SIZE;
  int CHUNK_SIZE_SHIFT = (int) (Math.log(CHUNK_SIZE) / Math.log(2));

  /**
   * Set a block and update all blocks around it
   *
   * @param localX The local x ie a value between 0 and {@link #CHUNK_SIZE}
   * @param localY The local y ie a value between 0 and {@link #CHUNK_SIZE}
   * @param material The material of the new block
   * @return The new block, only {@code null} if {@code material} parameter is {@code null}
   */
  @Contract("_,_,!null->!null;_,_,null->null")
  Block setBlock(int localX, int localY, @Nullable Material material);

  /**
   * @param localX The local x ie a value between 0 and {@link #CHUNK_SIZE}
   * @param localY The local y ie a value between 0 and {@link #CHUNK_SIZE}
   * @param material The material of the new block
   * @param update If the texture of this chunk should be updated
   * @return The new block, only {@code null} if {@code material} parameter is {@code null}
   */
  @Contract("_,_,!null,_->!null;_,_,null,_->null")
  Block setBlock(int localX, int localY, @Nullable Material material, boolean update);

  /**
   * @param localX The local x ie a value between 0 and {@link #CHUNK_SIZE}
   * @param localY The local y ie a value between 0 and {@link #CHUNK_SIZE}
   * @param material The material of the new block
   * @param update If the texture of this chunk should be updated
   * @param prioritize
   * @return The new block, only {@code null} if {@code material} parameter is {@code null}
   */
  @Contract("_, _, !null, _, _ -> !null; _, _, null, _, _ -> null")
  Block setBlock(
      int localX, int localY, @Nullable Material material, boolean update, boolean prioritize);

  /**
   * @param localX The local x ie a value between 0 and {@link #CHUNK_SIZE}
   * @param localY The local y ie a value between 0 and {@link #CHUNK_SIZE}
   * @param block The new block
   * @param updateTexture If the texture of this chunk should be updated
   * @return The given block, equal to the {@code block} parameter
   */
  @Contract("_,_,!null,_->!null;_,_,null,_->null")
  Block setBlock(int localX, int localY, @Nullable Block block, boolean updateTexture);

  /**
   * @param localX The local x ie a value between 0 and {@link #CHUNK_SIZE}
   * @param localY The local y ie a value between 0 and {@link #CHUNK_SIZE}
   * @param block The new block
   * @param updateTexture If the texture of this chunk should be updated
   * @param prioritize If {@code updateTexture} is {@code true} then if this chunk be prioritized in
   *     the rendering order
   * @return The given block, equal to the {@code block} parameter
   */
  @Contract("_, _, !null, _, _ -> !null; _, _, null, _, _ -> null")
  Block setBlock(
      int localX, int localY, @Nullable Block block, boolean updateTexture, boolean prioritize);

  @Contract("_, _, !null, _, _, _ -> !null; _, _, null, _, _, _ -> null")
  Block setBlock(
      int localX,
      int localY,
      @Nullable Block block,
      boolean updateTexture,
      boolean prioritize,
      boolean sendUpdatePacket);

  /**
   * @param localX The local chunk x coordinate
   * @return The world coordinate from the local position as offset
   * @see CoordUtil#chunkToWorld(int, int)
   */
  int getWorldX(int localX);

  /**
   * @param localY The local chunk y coordinate
   * @return The world coordinate from the local position as offset
   * @see CoordUtil#chunkToWorld(int, int)
   */
  int getWorldY(int localY);

  /**
   * Force update of this chunk's texture and invariants
   *
   * <p>Prioritization will not be removed if already prioritized.
   *
   * @param prioritize If this chunk should be prioritized when rendering
   */
  void updateTexture(boolean prioritize);

  /**
   * Might cause a call to {@link #updateIfDirty()} if the chunk is marked as dirty
   *
   * @return The texture of this chunk
   */
  @Nullable
  TextureRegion getTextureRegion();

  /**
   * Will not call {@link #updateIfDirty()}
   *
   * @return If this chunk has a texture generated
   */
  boolean hasTextureRegion();

  /**
   * Force update of texture and recalculate internal variables This is usually called when the
   * dirty flag of the chunk is set and either {@link #isAllAir()} or {@link #getTextureRegion()}
   * called.
   */
  void updateIfDirty();

  void updateBlockLights();

  /** Mark this chunk as viewed during the current tick */
  void view();

  FrameBuffer getFbo();

  /**
   * @return The backing array of the chunk, might contain null elements
   */
  @NotNull
  Block[][] getBlocks();

  @NotNull
  BlockLight getBlockLight(int localX, int localY);

  @Nullable
  Block getRawBlock(int localX, int localY);

  @NotNull
  Array<TickingBlock> getTickingBlocks();

  /**
   * Might cause a call to {@link #updateIfDirty()} if the chunk is marked as dirty
   *
   * @return If all blocks in this chunk is air
   */
  boolean isAllAir();

  /**
   * @return If this chunk has not been unloaded
   */
  boolean isLoaded();

  /**
   * @return If this chunk has been fully loaded and has not been unloaded
   */
  boolean isValid();

  boolean isInvalid();

  /**
   * If {@code isAllowingUnloading} is {@code false} this chunk cannot be unloaded
   *
   * @param allowUnload If the chunk can be unloaded or not
   */
  void setAllowUnload(boolean allowUnload);

  /**
   * @return {@code true} if all the {@link Direction#CARDINAL} neighbors are loaded
   */
  boolean isNeighborsLoaded();

  /**
   * If {@link ClientMain#getPlayer()} is in this chunk, unloading should never be allowed
   *
   * @return If this chunk is allowed to be unloaded
   */
  boolean isAllowingUnloading();

  @NotNull
  World getWorld();

  @NotNull
  ChunkColumn getChunkColumn();

  int getChunkX();

  int getChunkY();

  long getCompactLocation();

  int getWorldX();

  int getWorldY();

  long getLastViewedTick();

  boolean shouldSave();

  Stream<@Nullable Block> stream();

  @NotNull
  Block getBlock(int localX, int localY);

  Array<Entity> getEntities();

  @NotNull
  ChunkBody getChunkBody();

  /**
   * @return if this chunk is dirty
   */
  boolean isDirty();

  /** Mark chunk as dirty */
  void dirty();

  boolean load(ProtoWorld.Chunk protoChunk);

  @NotNull
  ProtoWorld.Chunk save();

  @NotNull
  ProtoWorld.Chunk saveBlocksOnly();
}
