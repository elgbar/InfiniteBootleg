package no.elg.infiniteBootleg.world.blocks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.Body
import no.elg.infiniteBootleg.api.HUDDebuggable
import no.elg.infiniteBootleg.api.Savable
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.block
import no.elg.infiniteBootleg.protobuf.entityOrNull
import no.elg.infiniteBootleg.protobuf.material
import no.elg.infiniteBootleg.util.CheckableDisposable
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.isInsideChunk
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.Material.Companion.fromProto
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.world.World

interface Block : CheckableDisposable, HUDDebuggable, Savable<ProtoWorld.Block> {

  val texture: RotatableTextureRegion?
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

  override fun hudDebug(): String {
    return "Block $material, pos ${stringifyCompactLoc(this)}"
  }

  companion object {

    /**
     * Texture size of block
     */
    const val BLOCK_SIZE = 16

    /**
     * Size of block in world coordinates
     */
    const val BLOCK_SIZE_WORLD = 1

    val Block.compactWorldLoc: Long get() = compactLoc(worldX, worldY)
    val Block.worldX: WorldCoord get() = chunk.getWorldX(localX)
    val Block.worldY: WorldCoord get() = chunk.getWorldY(localY)

    /**
     * Get the chunk of this block or the current valid chunk, which might be different from the chunk of this block or null if there is no longer a valid chunk for this block
     */
    val Block.validChunk: Chunk? get() = chunk.takeIf(Chunk::isValid) ?: world.getChunk(chunk.compactLocation, load = false)
    val Block.validChunkOrLoad: Chunk? get() = chunk.takeIf(Chunk::isValid) ?: world.getChunk(chunk.compactLocation, load = true)

    /**
     * Find all entities in the block
     */
    fun Block.queryEntities(callback: ((Set<Pair<Body, Entity>>) -> Unit)) =
      world.worldBody.queryEntities(worldX, worldY, worldX + BLOCK_SIZE_WORLD, worldY + BLOCK_SIZE_WORLD, callback)

    /**
     * Remove this block by setting it to air
     */
    fun Block.remove(updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true) {
      val validChunk = this.validChunk ?: return
      if (validChunk.getRawBlock(localX, localY) === this) {
        validChunk.removeBlock(
          localX = localX,
          localY = localY,
          updateTexture = updateTexture,
          prioritize = prioritize,
          sendUpdatePacket = sendUpdatePacket
        )
      }
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

    fun Block?.materialOrAir(): Material = this?.material ?: Material.AIR

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
      if (material === Material.AIR) {
        return null
      }
      return material.createBlock(world, chunk, localX, localY, protoBlock.entityOrNull)
    }

    fun save(material: Material): ProtoWorld.Block.Builder =
      block {
        this.material = material {
          ordinal = material.ordinal
        }
      }.toBuilder()
  }
}
