package no.elg.infiniteBootleg.screens.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.screens.ScreenRenderer
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block
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
  private val airItem = Item(Material.AIR, 0u, 0u)

  fun render(screenRenderer: ScreenRenderer, world: World?) {
    try {
      val entity = world?.engine?.getEntitiesFor(selectedMaterialComponentFamily)?.firstOrNull() ?: return
      val item = entity.selectedItem ?: airItem
      val texture = item.material.textureRegion ?: KAssets.breakingBlockTexture
      with(screenRenderer) {
        batch.draw(texture.textureRegion, Gdx.graphics.width - x4Block, Gdx.graphics.height - x3Block, x2Block, x2Block)
        layout.setText(font, "${item.stock} / ${item.maxStock}", Color.WHITE, x10Block, Align.right, true)
        font.draw(batch, layout, Gdx.graphics.width - x10Block - blockScale / 2, Gdx.graphics.height - x4Block)
      }
    } catch (e: NullPointerException) {
      Main.logger().error("HUD failed to render due to ${e.javaClass::getSimpleName}: ${e.message}")
    }
  }
}
