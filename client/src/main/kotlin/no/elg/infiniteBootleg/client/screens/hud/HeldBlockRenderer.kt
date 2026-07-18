package no.elg.infiniteBootleg.client.screens.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.ScreenRenderer
import no.elg.infiniteBootleg.client.world.textureRegion
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.events.ContainerEvent
import no.elg.infiniteBootleg.core.events.HotbarItemScrolled
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.api.RegisteredEventListener
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.items.Item.Companion.displayName
import no.elg.infiniteBootleg.core.items.Item.Companion.stockText
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem

class HeldBlockRenderer : Disposable {

  private val blockScale = Block.BLOCK_TEXTURE_SIZE * ClientMain.scale
  private val x2Block = blockScale * 2f
  private val x3Block = blockScale * 3f
  private val x4Block = blockScale * 4f
  private val x10Block = blockScale * 10f

  private val layout = GlyphLayout()

  private var lastItem: Item? = null
  private var updateItem: Boolean = true

  private var containerChanged: RegisteredEventListener = EventManager.registerListener<ContainerEvent.ContentChanged> {
    updateItem = true
  }
  private var selectedItemChange: RegisteredEventListener = EventManager.registerListener<HotbarItemScrolled> {
    updateItem = true
  }

  fun render(screenRenderer: ScreenRenderer, world: ClientWorld) {
    with(screenRenderer) {
      val currItem = lastItem
      if (currItem == null || updateItem) {
        val entity = world.controlledPlayerEntities.firstOrNull() ?: return
        val item = entity.selectedItem ?: return
        updateItem = false
        lastItem = item

        val stockText: String = if (item.element.stateless) {
          entity.containerOrNull?.let { "${item.stock} (${it.count(item.element)})" } ?: item.stockText
        } else {
          item.stockText
        }
        layout.setText(font, "${item.displayName}\n$stockText", Color.WHITE, x10Block, Align.center, true)
      }
      val texture = currItem?.element?.textureRegion?.textureRegionOrNull ?: ClientMain.inst().assets.breakableBlockTexture.textureRegion

      batch.draw(texture, Gdx.graphics.width - x4Block, Gdx.graphics.height - x3Block, x2Block, x2Block)
      font.draw(batch, layout, Gdx.graphics.width - x10Block - blockScale / 2, Gdx.graphics.height - x4Block)
    }
  }

  override fun dispose() {
    containerChanged.removeListener()
    selectedItemChange.removeListener()
  }
}
