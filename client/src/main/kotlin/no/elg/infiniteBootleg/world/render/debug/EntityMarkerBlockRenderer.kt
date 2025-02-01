package no.elg.infiniteBootleg.world.render.debug

import no.elg.infiniteBootleg.Settings.debugEntityMarkerBlocks
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.util.isMarkerBlock
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion

class EntityMarkerBlockRenderer(worldRender: ClientWorldRender) : SingleBlockDebugRenderer(worldRender) {

  override val isActive: Boolean get() = debugEntityMarkerBlocks
  override val texture: RotatableTextureRegion = ClientMain.inst().assets.handTexture

  override fun shouldRender(block: Block): Boolean = block.isMarkerBlock()
}
