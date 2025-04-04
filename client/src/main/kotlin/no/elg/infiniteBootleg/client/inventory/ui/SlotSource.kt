package no.elg.infiniteBootleg.client.inventory.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.textureRegion
import no.elg.infiniteBootleg.core.inventory.container.Container
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.inventory.container.impl.AutoSortedContainer
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.net.ServerClient.Companion.sendServerBoundPackets
import no.elg.infiniteBootleg.core.net.serverBoundContainerUpdate
import no.elg.infiniteBootleg.core.world.blocks.Block

private val logger = KotlinLogging.logger {}

class SlotSource(actor: Actor, private val sourceSlot: InventorySlot) : DragAndDrop.Source(actor) {

  fun image(textureRegion: TextureRegionDrawable): Image =
    Image(textureRegion, Scaling.fit).also {
      it.setSize(DRAG_ICON_SIZE, DRAG_ICON_SIZE)
    }

  override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): Payload? {
    val srcItem = sourceSlot.item ?: return null

    if (srcItem.stock == 0u || sourceSlot.ownedContainer.container is AutoSortedContainer) {
      return null
    }

    val payload = Payload()
    payload.setObject(sourceSlot)

    val icon = srcItem.element.textureRegion ?: return null
    val textureRegion: TextureRegionDrawable = TextureRegionDrawable(icon.textureRegionOrNull ?: ClientMain.inst().assets.whiteTexture.textureRegion)

    val dragActor = image(textureRegion)
    dragActor.color = Color.WHITE.cpy().mul(0.75f, 0.75f, 0.75f, 1f)
    payload.dragActor = dragActor

    val validDragActor: Actor = image(textureRegion)
    payload.validDragActor = validDragActor

    val invalidDragActor: Actor = image(textureRegion)
    invalidDragActor.color = Color.WHITE.cpy().mul(1f, 0f, 0f, 1f)
    payload.invalidDragActor = invalidDragActor

    return payload
  }

  private fun sendContainerUpdate(vararg containers: OwnedContainer) {
    if (Main.isServerClient) {
      ClientMain.inst().serverClient.sendServerBoundPackets {
        containers.map(::serverBoundContainerUpdate)
      }
    }
  }

  override fun dragStop(
    event: InputEvent,
    x: Float,
    y: Float,
    pointer: Int,
    payload: Payload?,
    target: DragAndDrop.Target?
  ) {
    val sourceSlot = payload?.getObject() as? InventorySlot ?: return
    val targetSlot = target?.actor?.userObject as? InventorySlot ?: return
    val splitStack = event.button == Input.Buttons.RIGHT
    if (targetSlot.ownedContainer == sourceSlot.ownedContainer) {
      val updatedContainer = sameContainer(sourceSlot.ownedContainer.container, sourceSlot, targetSlot, splitStack)
      if (updatedContainer) {
        sendContainerUpdate(sourceSlot.ownedContainer)
      }
    } else {
      val updatedContainer = differentContainer(sourceSlot, targetSlot)
      if (updatedContainer) {
        sendContainerUpdate(sourceSlot.ownedContainer, targetSlot.ownedContainer)
      }
    }
  }

  private fun sameContainer(container: Container, sourceSlot: InventorySlot, targetSlot: InventorySlot, splitStack: Boolean): Boolean {
    if (targetSlot.index == sourceSlot.index) {
      logger.debug { "Dragging to same slot, ignoring" }
      return false
    }

    val draggingItem = sourceSlot.item ?: return false
    val targetItem = targetSlot.item ?: if (splitStack) draggingItem.element.toItem(stock = 0u) else null
    if (targetItem?.element != draggingItem.element) {
      container.swap(targetSlot.index, sourceSlot.index)
    } else {
      // cannot split stack when stock is size 1
      val stockToMove = if (splitStack && draggingItem.stock > 1u) (draggingItem.stock / 2u) else draggingItem.stock
      val newItems = targetItem.add(stockToMove)
      val usages = if (newItems.size == 2) {
        // stock did not fit into the target item, we cannot move the rest
        val notAdded = newItems.last()
        stockToMove - notAdded.stock
      } else {
        // stock fit into the target item
        stockToMove
      }
      val targetUpdatedItem = newItems.firstOrNull()
      val sourceUpdatedItem = draggingItem.remove(usages)

      container[targetSlot.index] = targetUpdatedItem
      container[sourceSlot.index] = sourceUpdatedItem
    }
    return true
  }

  private fun differentContainer(sourceSlot: InventorySlot, targetSlot: InventorySlot): Boolean {
    val sourceContainer = sourceSlot.ownedContainer.container
    val targetContainer = targetSlot.ownedContainer.container

    val sourceItem = sourceSlot.item ?: return false
    val targetItem = targetSlot.item

    if (targetItem == null || targetItem.element != sourceItem.element) {
      targetContainer[targetSlot.index] = sourceItem
      sourceContainer[sourceSlot.index] = targetItem
    } else if (targetItem.element == sourceItem.element) {
      val change = targetItem.add(sourceItem.stock)
      sourceContainer.remove(sourceSlot.index)
      targetContainer[targetSlot.index] = change.firstOrNull()
      val remaining = targetContainer.add(change.drop(1))

      if (remaining.isNotEmpty()) {
        if (remaining.size == 1) {
          sourceContainer[sourceSlot.index] = remaining.single()
        } else {
          val remainingRemaining = sourceContainer.add(remaining)
          if (remainingRemaining.isNotEmpty()) {
            // wtf
            logger.error { "Remaining remaining (???): $remainingRemaining" }
          }
        }
      }
    }
    return true
  }

  companion object {
    const val DRAG_ICON_SIZE = Block.BLOCK_TEXTURE_SIZE * 3f
  }
}
