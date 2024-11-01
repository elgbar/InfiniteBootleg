package no.elg.infiniteBootleg.world.blocks

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.EntityRemoveListener
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block.Companion.remove
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkImpl.Companion.AIR_BLOCK_PROTO
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.material
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion

/**
 * An adapter block to represent an entity in the chunk world.
 *
 * This type of block should be set with [EntityMarkerBlock.replaceBlock] and should not be created directly.
 *
 * The block will be automatically removed if the entity is deleted or the entity is no longer in this world block
 * @see no.elg.infiniteBootleg.world.ecs.system.block.UpdateGridBlockSystem
 */
class EntityMarkerBlock(
  override val chunk: Chunk,
  override val localX: LocalCoord,
  override val localY: LocalCoord,
  override val entity: Entity
) : Block {

  private var removeEntityListener: EntityListener? =
    EntityRemoveListener { if (it === entity) removeEntityMarker(async = false) }

  init {
    removeEntityListener?.also { world.engine.addEntityListener(it) }
  }

  fun removeEntityMarker(async: Boolean, chunk: Chunk? = null) {
    fun remove() {
      if (chunk != null) {
        chunk.removeBlock(localX, localY, updateTexture = false, prioritize = false, sendUpdatePacket = false)
      } else {
        remove(updateTexture = false, prioritize = false, sendUpdatePacket = false)
      }
    }

    if (async) {
      launchOnAsync {
        remove()
      }
    } else {
      remove()
    }
  }

  override val material: Material get() = entity.material

  override fun dispose() {
    removeEntityListener?.also {
      world.postBox2dRunnable {
        world.engine.removeEntityListener(it)
      }
      removeEntityListener = null
    }
  }

  override fun save(): ProtoWorld.Block = AIR_BLOCK_PROTO

  override val texture: RotatableTextureRegion? get() = null

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

  override fun toString(): String {
    return "EntityMarkerBlock(chunk=$chunk, localX=$localX, localY=$localY, entity=$entity, material=$material)"
  }

  companion object {
    /**
     * Replace a block with a [EntityMarkerBlock].
     *
     * @return the new block
     */
    fun replaceBlock(block: Block, entity: Entity): EntityMarkerBlock {
      return EntityMarkerBlock(block.chunk, block.localX, block.localY, entity).let { emb ->
        emb.world.setBlock(emb) as? EntityMarkerBlock ?: error("Failed to set marker block at ${emb.worldX}, ${emb.worldY} was given a ${emb::class}")
      }
    }
  }
}
