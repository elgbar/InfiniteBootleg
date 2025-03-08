package no.elg.infiniteBootleg.client.world.render.debug

import com.badlogic.gdx.graphics.g2d.Batch
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.chunkX
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn.Companion.FeatureFlag
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

class TopBlockChunkColumnRenderer(worldRender: ClientWorldRender) : SingleBlockDebugRenderer(worldRender) {
  val chunkColumnsManager = worldRender.world.chunkColumnsManager

  override val alpha: Float = 1f
  override val isActive: Boolean get() = Settings.renderTopBlocks
  override val texture: RotatableTextureRegion = ClientMain.inst().assets.spellTexture

  override fun beforeRender(block: Block, batch: Batch) {
    val chunkColumn = chunkColumnsManager.getChunkColumn(block.chunkX)
    val localX = block.localX
    val topBlockSolid = chunkColumn.topBlockHeight(localX, FeatureFlag.SOLID_FLAG)
    val topBlockLight = chunkColumn.topBlockHeight(localX, FeatureFlag.BLOCKS_LIGHT_FLAG)

    val worldY = block.worldY
    if (topBlockSolid == topBlockLight && topBlockSolid == worldY) {
      batch.setColor(0f, 1f, 1f, 1f)
    } else if (topBlockSolid == worldY) {
      batch.setColor(0f, 1f, 0f, 1f)
    } else if (topBlockLight == worldY) {
      batch.setColor(0f, 0f, 1f, 1f)
    }
  }

  override fun shouldRender(block: Block): Boolean {
    val chunkColumn = chunkColumnsManager.getChunkColumn(block.chunkX)
    val localX = block.localX
    val topBlockSolid = chunkColumn.topBlockHeight(localX, FeatureFlag.SOLID_FLAG)
    val topBlockLight = chunkColumn.topBlockHeight(localX, FeatureFlag.BLOCKS_LIGHT_FLAG)

    val worldY = block.worldY
    return topBlockSolid == worldY || topBlockLight == worldY
  }
}
