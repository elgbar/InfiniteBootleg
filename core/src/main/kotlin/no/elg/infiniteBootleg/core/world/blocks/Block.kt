package no.elg.infiniteBootleg.core.world.blocks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.Body
import no.elg.infiniteBootleg.core.api.HUDDebuggable
import no.elg.infiniteBootleg.core.api.Savable
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.compactLoc
import no.elg.infiniteBootleg.core.util.isInsideChunk
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.Material.Companion.asProto
import no.elg.infiniteBootleg.core.world.Material.Companion.fromProto
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.core.world.world.World.Companion.BLOCK_SIZE
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.block
import no.elg.infiniteBootleg.protobuf.entityOrNull

interface Block :
  CheckableDisposable,
  HUDDebuggable,
  Savable<ProtoWorld.Block> {

  val material: Material
  val chunk: Chunk

  /**
   * @return The offset/local position of this block within its chunk
   */
  val localX: LocalCoord

  /**
   * @return The offset/local position of this block within its chunk
   */
  val localY: LocalCoord

  /**
   * Connected blocks to ashley engine
   */
  val entity: Entity?

  val world: World get() = chunk.world

  val valid: Boolean get() = chunk.isValid && !isDisposed

  override val isDisposed: Boolean get() = chunk.getRawBlock(localX, localY) !== this

  override fun hudDebug(): String = "Block $material, pos ${stringifyCompactLoc(this)}"

  companion object {

    /**
     * Texture size of block
     */
    const val BLOCK_TEXTURE_SIZE = 16

    const val BLOCK_TEXTURE_SIZE_F = BLOCK_TEXTURE_SIZE.toFloat()
    const val HALF_BLOCK_TEXTURE_SIZE_F = BLOCK_TEXTURE_SIZE_F * 0.5f

    inline val Block.compactWorldLoc: Long get() = compactLoc(worldX, worldY)
    inline val Block.worldX: WorldCoord get() = chunk.chunkX.chunkToWorld(localX)
    inline val Block.worldY: WorldCoord get() = chunk.chunkY.chunkToWorld(localY)
    inline val Block.chunkX: WorldCoord get() = chunk.chunkX
    inline val Block.chunkY: WorldCoord get() = chunk.chunkY

    /**
     * Get the chunk of this block or the current valid chunk, which might be different from the chunk of this block or null if there is no longer a valid chunk for this block
     */
    val Block.validChunk: Chunk? get() = chunk.takeIf(Chunk::isValid) ?: world.getChunk(chunk.compactLocation, load = false)
    val Block.validChunkOrLoad: Chunk? get() = chunk.takeIf(Chunk::isValid) ?: world.getChunk(chunk.compactLocation, load = true)

    /**
     * Find all entities in the block
     */
    fun Block.queryEntities(callback: ((Set<Pair<Body, Entity>>) -> Unit)) = world.worldBody.queryEntities(worldX, worldY, worldX + BLOCK_SIZE, worldY + BLOCK_SIZE, callback)

    /**
     * Remove this block by setting it to air
     *
     * @return The valid chunk of this block
     */
    fun Block.remove(updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true): Chunk? {
      val validChunk = this.validChunk ?: return null
      if (validChunk.getRawBlock(localX, localY) === this) {
        validChunk.removeBlock(
          localX = localX,
          localY = localY,
          updateTexture = updateTexture,
          prioritize = prioritize,
          sendUpdatePacket = sendUpdatePacket
        )
        return validChunk
      }
      return null
    }

    /**
     * Remove this block by setting it to air, done asynchronous
     */
    fun Block.removeAsync(updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true, postRemove: () -> Unit = {}) {
      launchOnAsync {
        remove(updateTexture, prioritize, sendUpdatePacket)
        postRemove()
      }
    }

    fun Block.getRawRelative(dir: Direction, load: Boolean = true): Block? {
      val newLocalX = localX + dir.dx
      val newLocalY = localY + dir.dy
      return if (isInsideChunk(newLocalX, newLocalY)) {
        chunk.getRawBlock(newLocalX, newLocalY)
      } else {
        val newWorldX: WorldCoord = worldX + dir.dx
        val newWorldY: WorldCoord = worldY + dir.dy
        world.getRawBlock(newWorldX, newWorldY, load)
      }
    }

    fun Block.forEachNeighbor(load: Boolean = true, func: (Block) -> Unit) {
      for (dir in Direction.NEIGHBORS) {
        val block = getRawRelative(dir, load)
        if (block != null) {
          func(block)
        }
      }
    }

    fun Block.sequenceOfNeighbors(load: Boolean = true): Sequence<Block> =
      sequence {
        for (dir in Direction.NEIGHBORS) {
          val block = getRawRelative(dir, load)
          if (block != null) {
            yield(block)
          }
        }
      }

    fun Block?.materialOrAir(): Material = this?.material ?: Material.Air

    fun fromProto(
      world: World,
      chunk: Chunk,
      localX: LocalCoord,
      localY: LocalCoord,
      protoBlock: ProtoWorld.Block?
    ): Block? {
      if (protoBlock == null) {
        return null
      }
      val material = protoBlock.material.fromProto()
      if (material === Material.Air) {
        return null
      }
      return material.createBlock(world, chunk, localX, localY, protoBlock.entityOrNull)
    }

    fun save(material: Material): ProtoWorld.Block.Builder =
      block {
        this.material = material.asProto()
      }.toBuilder()
  }
}
