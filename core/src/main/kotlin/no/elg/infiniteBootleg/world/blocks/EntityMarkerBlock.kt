package no.elg.infiniteBootleg.world.blocks

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.EntityRemoveListener
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block.Companion.remove
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkImpl.Companion.AIR_BLOCK_BUILDER
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.material
import no.elg.infiniteBootleg.world.render.RotatableTextureRegion
import no.elg.infiniteBootleg.world.world.World

class EntityMarkerBlock(
  override val chunk: Chunk,
  override val world: World,
  override val localX: Int,
  override val localY: Int,
  override val entity: Entity
) : Block {

  private var removeEntityListener: EntityListener? = null

  init {
    val world = Main.inst().world ?: throw IllegalStateException("World is null")
    world.postBox2dRunnable {
      removeEntityListener = EntityRemoveListener { if (it === entity) remove() }
      world.engine.addEntityListener(removeEntityListener)
    }
  }

  fun remove() {
    remove(updateTexture = false, prioritize = false, sendUpdatePacket = false)
  }

  override val material: Material = entity.material

  override var isDisposed: Boolean = false
    private set

  override fun dispose() {
    isDisposed = true

    Main.inst().world?.postBox2dRunnable {
      Main.inst().world?.engine?.removeEntityListener(removeEntityListener)
    }
    removeEntityListener = null
  }

  override fun save(): ProtoWorld.Block.Builder = AIR_BLOCK_BUILDER

  override val texture: RotatableTextureRegion? = if (debugEntityMarkerBlocks) KAssets.handTexture else null
  override fun load(protoBlock: ProtoWorld.Block) {}

  companion object {
    var debugEntityMarkerBlocks = false
    fun toggleDebugEntityMarkerBlocks() {
      debugEntityMarkerBlocks = !debugEntityMarkerBlocks
    }

    fun fromOtherBlock(block: Block, entity: Entity): EntityMarkerBlock = EntityMarkerBlock(block.chunk, block.world, block.localX, block.localY, entity)
  }
}
