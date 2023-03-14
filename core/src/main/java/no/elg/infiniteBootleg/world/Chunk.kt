package no.elg.infiniteBootleg.world

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.CheckableDisposable
import no.elg.infiniteBootleg.api.Ticking
import no.elg.infiniteBootleg.events.chunks.ChunkLightUpdatedEvent
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.blocks.TickingBlock
import no.elg.infiniteBootleg.world.box2d.ChunkBody
import org.jetbrains.annotations.Contract
import java.util.stream.Stream
import kotlin.math.ln

/**
 * A piece of the world
 *
 * @author Elg
 */
interface Chunk : Iterable<Block>, Ticking, CheckableDisposable, Comparable<Chunk> {

  /**
   * @return The backing array of the chunk, might contain null elements
   */
  val blocks: Array<Array<Block?>>

  val tickingBlocks: GdxArray<TickingBlock>

  /**
   * Might cause a call to [.updateIfDirty] if the chunk is marked as dirty
   *
   * @return If all blocks in this chunk is air
   */
  val isAllAir: Boolean

  /**
   * @return If this chunk has not been unloaded
   */
  val isNotDisposed: Boolean

  /**
   * @return If this chunk has been fully loaded and has not been unloaded
   */
  val isValid: Boolean

  /**
   * @return negated [.isValid]
   */
  val isInvalid: Boolean

  /**
   * @return `true` if all the [Direction.CARDINAL] neighbors are loaded
   */
  val areNeighborsLoaded: Boolean

  /**
   * If players are in this chunk, unloading should never be allowed
   *
   * @return If this chunk is allowed to be unloaded
   */
  val isAllowedToUnload: Boolean

  val world: World

  val chunkColumn: ChunkColumn

  val chunkX: Int

  val chunkY: Int

  val compactLocation: Long

  val worldX: Int

  val worldY: Int

  val lastViewedTick: Long

  //  Future<Array<Entity>> getEntities();
  val chunkBody: ChunkBody

  /**
   * @return if this chunk is dirty
   */
  val isDirty: Boolean

  /**
   * Might cause a call to [updateIfDirty] if the chunk is marked as dirty
   *
   * @return The texture of this chunk
   */
  val textureRegion: TextureRegion?

  /**
   * @return The backing [FrameBuffer] which holds the texture of this chunk. Will be null if
   * the chunk is disposed, never null otherwise.
   */
  val fbo: FrameBuffer?

  /**
   * @param localX The local x ie a value between 0 and [CHUNK_SIZE]
   * @param localY The local y ie a value between 0 and [CHUNK_SIZE]
   * @param material The material of the new block
   * @param updateTexture If the texture of this chunk should be updated
   * @param prioritize If an update should be sent when in multiplayer
   * @return The new block, only `null` if `material` parameter is `null`
   */
  @Contract("_, _, !null, _, _, _ -> !null; _, _, null, _, _, _ -> null")
  fun setBlock(localX: Int, localY: Int, material: Material?, updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true): Block?

  /**
   * @param localX The local x ie a value between 0 and [CHUNK_SIZE]
   * @param localY The local y ie a value between 0 and [CHUNK_SIZE]
   * @param block The new block
   * @param updateTexture If the texture of this chunk should be updated
   * @param prioritize If `updateTexture` is `true` then if this chunk be prioritized in the rendering order
   * @param sendUpdatePacket If an update should be sent when in multiplayer
   * @return The given block, equal to the `block` parameter
   */
  @Contract("_, _, !null, _, _, _ -> !null; _, _, null, _, _, _ -> null")
  fun setBlock(localX: Int, localY: Int, block: Block?, updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true): Block?

  /**
   * Force update of this chunk's texture and invariants
   *
   * Prioritization will not be removed if already prioritized.
   *
   * @param prioritize If this chunk should be prioritized when rendering
   */
  fun updateTexture(prioritize: Boolean)

  /**
   * Will not call [updateIfDirty]
   *
   * @return If this chunk has a texture generated
   */
  fun hasTextureRegion(): Boolean

  /**
   * Force update of texture and recalculate internal variables This is usually called when the
   * dirty flag of the chunk is set and either [isAllAir] or [textureRegion]
   * called.
   */
  fun updateIfDirty()

  /** Update the light of the chunk  */
  fun updateBlockLights(localX: Int = ChunkLightUpdatedEvent.CHUNK_CENTER, localY: Int = ChunkLightUpdatedEvent.CHUNK_CENTER, dispatchEvent: Boolean = true)

  /** Mark this chunk as viewed during the current tick  */
  fun view()

  /**
   * @param localX The local chunk x coordinate
   * @return The world coordinate from the local position as offset
   * @see CoordUtilKt.chunkToWorld
   */
  fun getWorldX(localX: Int): Int

  /**
   * @param localY The local chunk y coordinate
   * @return The world coordinate from the local position as offset
   * @see CoordUtilKt.chunkToWorld
   */
  fun getWorldY(localY: Int): Int

  fun getBlockLight(localX: Int, localY: Int): BlockLight

  fun getRawBlock(localX: Int, localY: Int): Block?

  fun getBlock(localX: Int, localY: Int): Block

  /**
   * If `isAllowingUnloading` is `false` this chunk cannot be unloaded
   *
   * @param allowUnload If the chunk can be unloaded or not
   */
  fun setAllowUnload(allowUnload: Boolean)

  fun shouldSave(): Boolean

  fun stream(): Stream<Block>

  /** Mark chunk as dirty  */
  fun dirty()

  fun load(protoChunk: ProtoWorld.Chunk): Boolean

  fun save(): ProtoWorld.Chunk

  fun saveBlocksOnly(): ProtoWorld.Chunk

  companion object {
    const val CHUNK_SIZE = 32
    const val CHUNK_TEXTURE_SIZE = CHUNK_SIZE * Block.BLOCK_SIZE
    val CHUNK_SIZE_SHIFT = (ln(CHUNK_SIZE.toDouble()) / ln(2.0)).toInt()
  }
}
