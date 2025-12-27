package no.elg.infiniteBootleg.client.world.render.debug

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings.renderInvisibleBlocks
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

class InvisibleBlockRenderer(worldRender: ClientWorldRender) : UnitSingleBlockDebugRenderer(worldRender) {

  override val isActive: Boolean
    get() = renderInvisibleBlocks

  override fun beforeRender(block: Block, batch: Batch) {
    batch.color = when (block.material) {
      Material.Door -> Color.CORAL
      else -> Color.WHITE
    }
  }

  override val texture: RotatableTextureRegion = ClientMain.inst().assets.visibleAirTexture

  override fun shouldRender(block: Block): Boolean = block.material.invisibleBlock
}
