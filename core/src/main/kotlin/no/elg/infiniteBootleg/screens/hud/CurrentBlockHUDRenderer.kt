package no.elg.infiniteBootleg.screens.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import no.elg.infiniteBootleg.items.Item.Companion.labelText
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.ScreenRenderer
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.world.world.World

object CurrentBlockHUDRenderer {

  private val blockScale = Block.BLOCK_SIZE * ClientMain.scale
  private val x2Block = blockScale * 2f
  private val x3Block = blockScale * 3f
  private val x4Block = blockScale * 4f
  private val x10Block = blockScale * 10f

  private val layout = GlyphLayout()

  fun render(screenRenderer: ScreenRenderer, world: World) {
    val entity = world.controlledPlayerEntities.firstOrNull() ?: return
    val item = entity.selectedItem ?: return
    val texture = item.element.textureRegion?.textureRegionOrNull ?: Main.inst().assets.breakableBlockTexture.textureRegion
    with(screenRenderer) {
      batch.draw(texture, Gdx.graphics.width - x4Block, Gdx.graphics.height - x3Block, x2Block, x2Block)
      layout.setText(font, item.labelText, Color.WHITE, x10Block, Align.right, true)
      font.draw(batch, layout, Gdx.graphics.width - x10Block - blockScale / 2, Gdx.graphics.height - x4Block)
    }
  }
}
