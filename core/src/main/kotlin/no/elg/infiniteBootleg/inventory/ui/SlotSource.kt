package no.elg.infiniteBootleg.inventory.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.impl.AutoSortedContainer
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.server.ServerClient.Companion.sendServerBoundPackets
import no.elg.infiniteBootleg.server.serverBoundContainerUpdate
import no.elg.infiniteBootleg.util.toVector2i
import no.elg.infiniteBootleg.world.blocks.Block

class SlotSource(actor: Actor, private val sourceSlot: InventorySlot) : DragAndDrop.Source(actor) {

  override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): Payload? {
    val srcTs = sourceSlot.item ?: return null

    if (srcTs.stock == 0u || sourceSlot.container is AutoSortedContainer) {
      return null
    }

    val payload = Payload()
    payload.setObject(sourceSlot)

    val icon = srcTs.element.textureRegion ?: return null
    val textureRegion = TextureRegionDrawable(icon.textureRegionOrNull ?: ClientMain.inst().assets.whiteTexture.textureRegion)

    fun image(): Image =
      Image(textureRegion, Scaling.fit).also {
        it.setSize(DRAG_ICON_SIZE, DRAG_ICON_SIZE)
      }

    val dragActor = image()
    dragActor.color = Color.WHITE.cpy().mul(0.75f, 0.75f, 0.75f, 1f)
    payload.dragActor = dragActor

    val validDragActor: Actor = image()
    payload.validDragActor = validDragActor

    val invalidDragActor: Actor = image()
    invalidDragActor.color = Color.WHITE.cpy().mul(1f, 0f, 0f, 1f)
    payload.invalidDragActor = invalidDragActor

    return payload
  }

  private fun sendContainerUpdate(vararg containers: Container) {
    if (Main.isServerClient) {
      ClientMain.inst().serverClient.sendServerBoundPackets {
        containers.mapNotNull { container ->
          world?.worldContainerManager?.find(container)?.let { compactWorldPos ->
            serverBoundContainerUpdate(compactWorldPos.toVector2i(), container)
          }
        }
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
    if (targetSlot.container == sourceSlot.container) {
      val updatedContainer = sameContainer(sourceSlot.container, sourceSlot, targetSlot)
      if (updatedContainer) {
        sendContainerUpdate(sourceSlot.container)
      }
    } else {
      val updatedContainer = differentContainer(sourceSlot, targetSlot)
      if (updatedContainer) {
        sendContainerUpdate(sourceSlot.container, targetSlot.container)
      }
    }
  }

  private fun sameContainer(container: Container, sourceSlot: InventorySlot, targetSlot: InventorySlot): Boolean {
    if (targetSlot.index == sourceSlot.index) {
      Main.logger().debug("DAD", "Dragging to same slot, ignoring")
      return false
    }

    val targetItem = targetSlot.item
    val draggingItem = sourceSlot.item ?: return false
    if (targetItem?.element != draggingItem.element) {
      container.swap(targetSlot.index, sourceSlot.index)
    } else {
      val change = targetItem.change(draggingItem.stock.toInt())
      container.remove(sourceSlot.index)
      container.put(targetSlot.index, change.firstOrNull())
      container.add(change.drop(1))
    }
    return true
  }

  private fun differentContainer(sourceSlot: InventorySlot, targetSlot: InventorySlot): Boolean {
    val sourceContainer = sourceSlot.container
    val targetContainer = targetSlot.container

    val sourceItem = sourceSlot.item ?: return false
    val targetItem = targetSlot.item

    if (targetItem == null || targetItem.element != sourceItem.element) {
      targetContainer.put(targetSlot.index, sourceItem)
      sourceContainer.put(sourceSlot.index, targetItem)
    } else if (targetItem.element == sourceItem.element) {
      val change = targetItem.change(sourceItem.stock.toInt())
      sourceContainer.remove(sourceSlot.index)
      targetContainer.put(targetSlot.index, change.firstOrNull())
      val remaining = targetContainer.add(change.drop(1))

      if (remaining.isNotEmpty()) {
        if (remaining.size == 1) {
          sourceContainer.put(sourceSlot.index, remaining.single())
        } else {
          val remainingRemaining = sourceContainer.add(remaining)
          if (remainingRemaining.isNotEmpty()) {
            // wtf
            Main.logger().error("DAD", "Remaining remaining (???): $remainingRemaining")
          }
        }
      }
    }
    return true
  }

  companion object {
    const val DRAG_ICON_SIZE = Block.BLOCK_SIZE * 3f
  }
}
