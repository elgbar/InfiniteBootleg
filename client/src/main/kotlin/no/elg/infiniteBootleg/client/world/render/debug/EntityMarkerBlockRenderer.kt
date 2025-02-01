package no.elg.infiniteBootleg.client.world.render.debug

import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings.debugEntityMarkerBlocks
import no.elg.infiniteBootleg.core.util.isMarkerBlock
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

class EntityMarkerBlockRenderer(worldRender: ClientWorldRender) : SingleBlockDebugRenderer(worldRender) {

  override val isActive: Boolean get() = debugEntityMarkerBlocks
  override val texture: RotatableTextureRegion = ClientMain.inst().assets.handTexture

  override fun shouldRender(block: Block): Boolean = block.isMarkerBlock()
}
