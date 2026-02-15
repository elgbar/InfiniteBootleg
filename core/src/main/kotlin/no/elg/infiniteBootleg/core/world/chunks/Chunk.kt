package no.elg.infiniteBootleg.core.world.chunks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.box2d.structs.b2BodyId
import no.elg.infiniteBootleg.core.util.BlockUnit
import no.elg.infiniteBootleg.core.util.BlockUnitF
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.BlockLight
import no.elg.infiniteBootleg.core.world.box2d.ChunkBody
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.CHUNK_SIZE
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import org.jetbrains.annotations.Contract
import java.util.concurrent.CompletableFuture
import kotlin.contracts.contract
import kotlin.math.ln

/**
 * A piece of the world
 *
 * @author Elg
 */
interface Chunk :
  Iterable<Block?>,
  CheckableDisposable,
  Comparable<Chunk> {

  /**
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
   * Sometimes we do not want to unload specific chunks, for example if there is a player in that chunk or it is the spawn chunk
   *
   * @return If this chunk is allowed to be unloaded
   */
  var allowedToUnload: Boolean

  val world: World

  val chunkColumn: ChunkColumn

  val chunkX: ChunkCoord

  val chunkY: ChunkCoord

  val compactLocation: ChunkCompactLoc

  val worldX: WorldCoord

  val worldY: WorldCoord

  val chunkBody: ChunkBody

  /**
   * @return if this chunk is dirty
   */
  val isDirty: Boolean

  /**
   * Force update of texture and recalculate internal variables This is usually called when the
   * dirty flag of the chunk is set and either [isAllAir] called.
   *
   * @return If this chunk was prioritized
   */
  fun updateIfDirty(): Boolean

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

  /** Update the light of the chunk  */
  fun updateAllBlockLights()

  fun getBlockLight(localX: LocalCoord, localY: LocalCoord): BlockLight

  fun getRawBlock(localX: LocalCoord, localY: LocalCoord): Block?

  /**
   * @param localX The local x ie a value between 0 and [Chunk.CHUNK_SIZE]
   * @param localY The local y ie a value between 0 and [Chunk.CHUNK_SIZE]
   * @return The block instance of the given coordinates, a new air block will be created if there is no existing block
   */
  fun getBlock(localX: LocalCoord, localY: LocalCoord): Block

  /**
   * An optimized version of [World.getBlock] that first checks if the block is in this chunk
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

  /** Mark chunk as dirty  */
  fun dirty(prioritize: Boolean = false)

  /**
   * Find all entities in the chunk. The order is arbitrary
   *
   * Calls [callback] for each entity found in the chunk
   */
  fun queryEachEntities(callback: ((b2BodyId, Entity) -> Unit))

  /**
   * Find all entities in the chunk. The order is arbitrary
   *
   * @param afterAllCallback a set of all unique entities in the chunk
   */
  fun queryAllEntities(afterAllCallback: (Set<Entity>) -> Unit)

  /**
   * @return If the chunk has been modified since creation
   */
  fun shouldSave(): Boolean

  fun load(protoChunk: ProtoWorld.Chunk): Boolean

  fun save(): CompletableFuture<ProtoWorld.Chunk>

  fun saveBlocksOnly(): ProtoWorld.Chunk

  companion object {
    /**
     * Chunks size in blocks
     */
    const val CHUNK_SIZE: BlockUnit = 16 // Note if changed, CHUNK_SIZE_SHIFT must be updated
    const val CHUNK_SIZE_F: BlockUnitF = CHUNK_SIZE.toFloat()
    const val CHUNK_TEXTURE_SIZE = CHUNK_SIZE * Block.BLOCK_TEXTURE_SIZE
    const val CHUNK_TEXTURE_SIZE_F = CHUNK_TEXTURE_SIZE.toFloat()
    const val CHUNK_TEXTURE_SIZE_HALF = CHUNK_TEXTURE_SIZE / 2
    const val CHUNK_SIZE_SHIFT = 4

    init {
      val calculatedShift = (ln(CHUNK_SIZE.toDouble()) / ln(2.0)).toInt()
      require(CHUNK_SIZE_SHIFT == calculatedShift) { "Chunk size shift have changed! Old: $CHUNK_SIZE_SHIFT, calculated: $calculatedShift" }
    }

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
