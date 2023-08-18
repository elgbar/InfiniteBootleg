package no.elg.infiniteBootleg.world.blocks

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.Settings.debugEntityMarkerBlocks
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.EntityRemoveListener
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block.Companion.remove
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkImpl.Companion.AIR_BLOCK_PROTO_BUILDER
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.material
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.world.World

class EntityMarkerBlock(
  override val chunk: Chunk,
  override val world: World,
  override val localX: LocalCoord,
  override val localY: LocalCoord,
  override val entity: Entity
) : Block {

  private var removeEntityListener: EntityListener?

  init {
    removeEntityListener = EntityRemoveListener { if (it === entity) removeEntityMarker() }.also {
      world.engine.addEntityListener(it)
    }
  }

  fun removeEntityMarker() {
    remove(updateTexture = false, prioritize = false, sendUpdatePacket = false)
  }

  override val material: Material = entity.material

  override var isDisposed: Boolean = false
    private set

  override fun dispose() {
    isDisposed = true

    removeEntityListener?.also {
      world.postBox2dRunnable {
        world.engine.removeEntityListener(it)
      }
      removeEntityListener = null
    }
  }

  override fun save(): ProtoWorld.Block.Builder = AIR_BLOCK_PROTO_BUILDER

  override val texture: RotatableTextureRegion? = if (debugEntityMarkerBlocks) KAssets.handTexture else null

  companion object {
    fun toggleDebugEntityMarkerBlocks() {
      debugEntityMarkerBlocks = !debugEntityMarkerBlocks
    }

    /**
     * Replace a block with a [EntityMarkerBlock].
     *
     * @return the new block
     */
    fun replaceBlock(block: Block, entity: Entity): EntityMarkerBlock {
      return EntityMarkerBlock(block.chunk, block.world, block.localX, block.localY, entity).let { emb ->
        emb.world.setBlock(emb) as? EntityMarkerBlock ?: error("Failed to set marker block at ${emb.worldX}, ${emb.worldY} was given a ${emb::class}")
      }
    }
  }
}
