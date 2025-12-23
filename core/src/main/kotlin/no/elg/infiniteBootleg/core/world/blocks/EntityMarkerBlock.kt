package no.elg.infiniteBootleg.core.world.blocks

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import no.elg.infiniteBootleg.core.util.EntityRemoveListener
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.remove
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.valid
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent.Companion.material
import no.elg.infiniteBootleg.core.world.ecs.components.tags.BrokenBlockTag.Companion.brokenBlock
import no.elg.infiniteBootleg.protobuf.ProtoWorld

/**
 * An adapter block to represent an entity in the chunk world.
 *
 * This type of block should be set with [EntityMarkerBlock.replaceBlock] and should not be created directly.
 *
 * The block will be automatically removed if the entity is deleted or the entity is no longer in this world block
 * @see no.elg.infiniteBootleg.core.world.ecs.system.block.UpdateGridBlockSystem
 */
class EntityMarkerBlock(override val chunk: Chunk, override val localX: LocalCoord, override val localY: LocalCoord, override val entity: Entity, val hardLink: Boolean) : Block {

  private var removeEntityListener: EntityListener? =
    EntityRemoveListener { if (it === entity) removeEntityMarker() }

  init {
    removeEntityListener?.also { world.engine.addEntityListener(it) }
  }

  fun removeEntityMarker() {
    remove(updateTexture = false, sendUpdatePacket = false)
  }

  override val material: Material get() = entity.material

  override fun dispose() {
    removeEntityListener?.also {
      world.postBox2dRunnable {
        world.engine.removeEntityListener(it)
      }
      removeEntityListener = null
    }
    if (hardLink) {
      entity.brokenBlock = true
    }
  }

  override fun save(): ProtoWorld.Block = ChunkImpl.AIR_BLOCK_PROTO

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is EntityMarkerBlock) return false

    if (chunk != other.chunk) return false
    if (world != other.world) return false
    if (localX != other.localX) return false
    if (localY != other.localY) return false
    if (entity != other.entity) return false

    return true
  }

  override fun hashCode(): Int {
    var result = chunk.hashCode()
    result = 31 * result + world.hashCode()
    result = 31 * result + localX
    result = 31 * result + localY
    result = 31 * result + entity.hashCode()
    return result
  }

  override fun toString(): String = "EntityMarkerBlock(chunk=$chunk, localX=$localX, localY=$localY, entity=$entity, material=$material)"

  companion object {
    /**
     * Replace a block with a [EntityMarkerBlock].
     *
     * @return the new block
     */
    fun replaceBlock(
      chunk: Chunk,
      localX: LocalCoord,
      localY: LocalCoord,
      entity: Entity,
      hardLink: Boolean
    ): EntityMarkerBlock? {
      require(chunk.valid()) { "Block must be in a valid chunk" }
      return EntityMarkerBlock(chunk, localX, localY, entity, hardLink).let { emb ->
        val replacedBlock = chunk.setBlock(localX, localY, emb, sendUpdatePacket = false)
        replacedBlock as? EntityMarkerBlock?
      }
    }
  }
}
