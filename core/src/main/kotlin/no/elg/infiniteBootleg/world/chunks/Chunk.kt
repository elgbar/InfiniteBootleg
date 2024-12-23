package no.elg.infiniteBootleg.world.chunks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.physics.box2d.Body
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.CheckableDisposable
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.BlockLight
import no.elg.infiniteBootleg.world.box2d.ChunkBody
import no.elg.infiniteBootleg.world.world.World
import org.jetbrains.annotations.Contract
import java.util.concurrent.CompletableFuture
import kotlin.contracts.contract
import kotlin.math.ln

/**
 * A piece of the world
 *
 * @author Elg
 */
interface Chunk : Iterable<Block?>, CheckableDisposable, Comparable<Chunk> {

  /**
   * Might cause a call to [updateIfDirty] if the chunk is marked as dirty
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
   * @return negated [isValid]
   */
  val isInvalid: Boolean

  /**
   * If players are in this chunk, unloading should never be allowed
   *
   * @return If this chunk is allowed to be unloaded
   */
  val isAllowedToUnload: Boolean

  val world: World

  val chunkColumn: ChunkColumn

  val chunkX: ChunkCoord

  val chunkY: ChunkCoord

  val compactLocation: ChunkCompactLoc

  val worldX: WorldCoord

  val worldY: WorldCoord

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
  val texture: Texture?

  /**
   * @return The backing [FrameBuffer] which holds the texture of this chunk. Will be null if
   * the chunk is disposed, never null otherwise.
   */
  val frameBuffer: FrameBuffer?

  /**
   * @param localX The local x ie a value between 0 and [CHUNK_SIZE]
   * @param localY The local y ie a value between 0 and [CHUNK_SIZE]
   * @param material The material of the new block
   * @param updateTexture If the texture of this chunk should be updated
   * @param prioritize If an update should be sent when in multiplayer
   * @return The new block, only `null` if `material` parameter is `null`
   */
  @Contract("_, _, !null, _, _, _ -> !null; _, _, null, _, _, _ -> null")
  fun setBlock(
    localX: LocalCoord,
    localY: LocalCoord,
    material: Material?,
    updateTexture: Boolean = true,
    prioritize: Boolean = false,
    sendUpdatePacket: Boolean = true
  ): Block?

  /**
   * @param localX The local x ie a value between 0 and [CHUNK_SIZE]
   * @param localY The local y ie a value between 0 and [CHUNK_SIZE]
   * @param block The new block
   * @param updateTexture If the texture of this chunk should be updated
   * @param prioritize If `updateTexture` is `true` then if this chunk be prioritized in the rendering order
   * @param sendUpdatePacket If an update should be sent when in multiplayer
   * @return The new block, equal to the `block` parameter (but not necessarily the same object)
   */
  @Contract("_, _, !null, _, _, _ -> !null; _, _, null, _, _, _ -> null")
  fun setBlock(
    localX: LocalCoord,
    localY: LocalCoord,
    block: Block?,
    updateTexture: Boolean = true,
    prioritize: Boolean = false,
    sendUpdatePacket: Boolean = true
  ): Block?

  /**
   * @param localX The local x ie a value between 0 and [CHUNK_SIZE]
   * @param localY The local y ie a value between 0 and [CHUNK_SIZE]
   * @param updateTexture If the texture of this chunk should be updated
   * @param prioritize If `updateTexture` is `true` then if this chunk be prioritized in the rendering order
   * @param sendUpdatePacket If an update should be sent when in multiplayer
   */
  @Contract("_, _, !null, _, _, _ -> !null; _, _, null, _, _, _ -> null")
  fun removeBlock(
    localX: LocalCoord,
    localY: LocalCoord,
    updateTexture: Boolean = true,
    prioritize: Boolean = false,
    sendUpdatePacket: Boolean = true
  )

  /**
   * Force update of this chunk's texture and invariants
   *
   * Prioritization will not be removed if already prioritized.
   *
   * @param prioritize If this chunk should be prioritized when rendering
   */
  fun updateTexture(prioritize: Boolean)

  /**
   * Will not update textures
   *
   * @return If this chunk has a texture generated
   */
  fun hasTexture(): Boolean
  fun queueForRendering(prioritize: Boolean)

  /** Update the light of the chunk  */
  fun updateAllBlockLights()

  /** Mark this chunk as viewed during the current tick  */
  fun view()

  /**
   * @param localX The local chunk x coordinate
   * @return The world coordinate from the local position as offset
   */
  fun getWorldX(localX: LocalCoord): Int

  /**
   * @param localY The local chunk y coordinate
   * @return The world coordinate from the local position as offset
   */
  fun getWorldY(localY: LocalCoord): Int

  fun getBlockLight(localX: LocalCoord, localY: LocalCoord): BlockLight

  fun getRawBlock(localX: LocalCoord, localY: LocalCoord): Block?

  /**
   * @param localX The local x ie a value between 0 and [Chunk.CHUNK_SIZE]
   * @param localY The local y ie a value between 0 and [Chunk.CHUNK_SIZE]
   * @return The block instance of the given coordinates, a new air block will be created if there is no existing block
   */
  fun getBlock(localX: LocalCoord, localY: LocalCoord): Block

  /**
   * An optimized version of [no.elg.infiniteBootleg.world.world.World.getBlock] that first checks if the block is in this chunk
   *
   * Note an air block will be created if the chunk is loaded and there is no other block at the
   * given location
   *
   * @param worldX    The x coordinate from world view
   * @param worldY    The y coordinate from world view
   * @param loadChunk If the chunk should be loaded if it is not already loaded
   * @return The block at the given x and y
   */
  fun getBlock(worldX: WorldCoord, worldY: WorldCoord, loadChunk: Boolean = true): Block?

  /**
   * If `isAllowingUnloading` is `false` this chunk cannot be unloaded
   *
   * @param allowUnload If the chunk can be unloaded or not
   */
  fun setAllowUnload(allowUnload: Boolean)

  /**
   * @return If the chunk has been modified since creation
   */
  fun shouldSave(): Boolean

  /** Mark chunk as dirty  */
  fun dirty()

  fun load(protoChunk: ProtoWorld.Chunk): Boolean

  fun save(): CompletableFuture<ProtoWorld.Chunk>

  fun saveBlocksOnly(): ProtoWorld.Chunk

  /**
   * Find all entities in the chunk
   */
  fun queryEntities(callback: ((Set<Pair<Body, Entity>>) -> Unit))

  companion object {
    /**
     * Chunks size in blocks
     */
    const val CHUNK_SIZE: LocalCoord = 16
    const val CHUNK_TEXTURE_SIZE = CHUNK_SIZE * Block.BLOCK_SIZE
    const val CHUNK_TEXTURE_SIZE_HALF = CHUNK_TEXTURE_SIZE / 2
    val CHUNK_SIZE_SHIFT = (ln(CHUNK_SIZE.toDouble()) / ln(2.0)).toInt()

    fun Chunk?.valid(): Boolean {
      contract { returns(true) implies (this@valid != null) }
      return this != null && this.isValid
    }

    fun Chunk?.invalid(): Boolean {
      contract { returns(false) implies (this@invalid != null) }
      return this == null || this.isInvalid
    }
  }
}
