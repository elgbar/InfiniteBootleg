package no.elg.infiniteBootleg.world.render.debug

import no.elg.infiniteBootleg.Settings.renderAirBlocks
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion

class AirBlockRenderer(worldRender: ClientWorldRender) : SingleBlockDebugRenderer(worldRender) {

  override val isActive: Boolean
    get() = renderAirBlocks

  override val texture: RotatableTextureRegion = Main.inst().assets.visibleAirTexture

  override fun shouldRender(block: Block): Boolean = block.material == Material.AIR
}
