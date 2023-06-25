package no.elg.infiniteBootleg.screens.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.screen.ScreenRenderer
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedItem
import no.elg.infiniteBootleg.world.ecs.selectedMaterialComponentFamily
import no.elg.infiniteBootleg.world.world.World

object CurrentBlockHUDRenderer {

  private val blockScale = Block.BLOCK_SIZE * ClientMain.SCALE
  private val x2Block = blockScale * 2f
  private val x3Block = blockScale * 3f
  private val x4Block = blockScale * 4f
  private val x10Block = blockScale * 10f
  private val layout = GlyphLayout()

  fun render(screenRenderer: ScreenRenderer, world: World?) {
    val entity = world?.engine?.getEntitiesFor(selectedMaterialComponentFamily)?.firstOrNull() ?: return
    val item = entity.selectedItem
    item?.material?.textureRegion?.also { texture ->
      with(screenRenderer) {
        batch.draw(texture, Gdx.graphics.width - x4Block, Gdx.graphics.height - x3Block, x2Block, x2Block)
        layout.setText(font, "${item.charge} / ${item.maxCharge}", Color.WHITE, x10Block, Align.right, true)
        font.draw(batch, layout, Gdx.graphics.width - x10Block - blockScale / 2, Gdx.graphics.height - x4Block)
      }
    }
  }
}
