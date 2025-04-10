package no.elg.infiniteBootleg.client.world.render.debug

import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.core.Settings.renderAirBlocks
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion

class AirBlockRenderer(worldRender: ClientWorldRender) : SingleBlockDebugRenderer(worldRender) {

  override val isActive: Boolean
    get() = renderAirBlocks

  override val texture: RotatableTextureRegion = ClientMain.inst().assets.visibleAirTexture

  override fun shouldRender(block: Block): Boolean = block.material == Material.Air
}
