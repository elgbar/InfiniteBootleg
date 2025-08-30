package no.elg.infiniteBootleg.client.screens.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import ktx.graphics.copy
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.ScreenRenderer
import no.elg.infiniteBootleg.client.world.textureRegion
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.events.ContainerEvent
import no.elg.infiniteBootleg.core.events.WorldTickedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.inventory.container.Container
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.items.Item.Companion.displayName
import no.elg.infiniteBootleg.core.util.ProgressHandler
import no.elg.infiniteBootleg.core.util.partitionMap
import no.elg.infiniteBootleg.core.util.withColor
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.BLOCK_TEXTURE_SIZE_F
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.ownedContainerOrNull
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion
import kotlin.math.absoluteValue

class ContainerChangeRenderer :
  Renderer,
  Disposable {

  private val screenRenderer: ScreenRenderer get() = ClientMain.inst().screenRenderer

  private val knownContainers = ObjectOpenHashSet<Container>()
  private val changeHandlers = ObjectOpenHashSet<EventDisplayData>()
  private val changeHandlersHold = ObjectOpenHashSet<ChangeToBeProcessed>()
  private val layout = GlyphLayout()

  /**
   * @return if the container is controlled by the currently controlled player
   */
  private fun isControlledContainer(container: Container): Boolean {
    if (container in knownContainers) return true
    val world = ClientMain.inst().world ?: return false
    return world.controlledPlayerEntities.asSequence().mapNotNull { it.ownedContainerOrNull }.any {
      if (it.container === container) {
        knownContainers += container
        return true
      } else {
        return false
      }
    }
  }

  private fun addForProcessing(maybeItem: Item?, remove: Boolean) {
    maybeItem?.let { item ->
      val toBeProcessed = ChangeToBeProcessed(item, remove)
      synchronized(changeHandlersHold) {
        changeHandlersHold += toBeProcessed
      }
    }
  }

  private val onContentChanged = EventManager.registerListener { (container, _, type): ContainerEvent.ContentChanged ->
    if (type != null && (type.addedItem != null || type.removedItem != null) && isControlledContainer(container)) {
      addForProcessing(type.addedItem, false)
      addForProcessing(type.removedItem, true)
    }
  }

  private val onWorldTick = EventManager.registerListener { e: WorldTickedEvent ->
    if (changeHandlersHold.isNotEmpty() && (changeHandlers.isEmpty() || e.tickId % (e.world.worldTicker.tps / LINES_PER_SECONDS) == 0L)) {
      synchronized(changeHandlersHold) {
        val data = changeHandlersHold.partitionMap { it -> it.item.element.displayName + it.item.stock }.mapNotNull { (_, items) ->
          val first = items.first()
          val sumOfStock = items.sumOf { (if (it.remove) -1 else 1) * it.item.stock.toInt() }
          if (sumOfStock != 0) {
            val copyToFit = first.item.copyToFit(sumOfStock.absoluteValue.toUInt())
            copyToFit to sumOfStock
          } else {
            // Sum of stock is 0, so we don't need to display anything
            null
          }
        }.sortedBy(Pair<*, Int>::second).mapIndexedNotNull { index, (item, sumOfStock) -> getData(item, sumOfStock < 0, index) }

        val progressHandler = ProgressHandler(DISPLAY_DURATION_SECONDS, Interpolation.sineOut, start = 1f)
        changeHandlers += EventDisplayData(data, progressHandler)
        changeHandlersHold.clear()
      }
    }
  }

  private fun render(data: ItemDisplayData, progress: Float, lastX: Float): Float {
    val yOffset = progress * Y_OFFSET
    val y = Gdx.graphics.height - yOffset
    val elementRenderX = lastX + BLOCK_TEXTURE_SIZE_F
    data.maybeTexture?.also { texture ->
      screenRenderer.batch.withColor(a = progress) { batch ->
        batch.draw(texture.textureRegion, elementRenderX, y - BLOCK_TEXTURE_SIZE_F, BLOCK_TEXTURE_SIZE_F, BLOCK_TEXTURE_SIZE_F)
      }
    }
    layout.setText(screenRenderer.font, data.text, data.color.copy(alpha = progress), 0f, Align.left, false)
    val x = elementRenderX + BLOCK_TEXTURE_SIZE_F * 1.5f
    screenRenderer.font.draw(screenRenderer.batch, layout, x, y - layout.height / 2)
    return x + layout.width
  }

  override fun render() {
    if (changeHandlers.isEmpty()) return
    val iterator = changeHandlers.iterator()
    while (iterator.hasNext()) {
      val (datas, progressHandler) = iterator.next()
      if (progressHandler.update(Gdx.graphics.deltaTime)) {
        iterator.remove()
      } else {
        val progress = progressHandler.progress
        var lastX = 0f
        for (data in datas) {
          lastX = render(data, progress, lastX)
        }
      }
    }
  }

  override fun dispose() {
    onContentChanged.removeListener()
    onWorldTick.removeListener()
  }

  fun getData(item: Item, remove: Boolean, xOffset: Int): ItemDisplayData? {
    val color = if (remove) Color.RED else Color.GREEN
    val text = "${if (remove) "-" else "+"}${item.stock} ${item.displayName}"
    return ItemDisplayData(text, color, item.element.textureRegion, xOffset)
  }

  companion object {
    data class ChangeToBeProcessed(val item: Item, val remove: Boolean)
    data class ItemDisplayData(val text: String, val color: Color, val maybeTexture: RotatableTextureRegion?, val xIndex: Int)
    data class EventDisplayData(val data: List<ItemDisplayData>, val progressHandler: ProgressHandler)

    /**
     * How far up (in pixels) the line will travel upwards
     */
    const val Y_OFFSET = 100f

    /**
     * How long to display a line
     */
    const val DISPLAY_DURATION_SECONDS = 3f

    /**
     * How many lines to be created each second
     */
    const val LINES_PER_SECONDS = 3
  }
}
