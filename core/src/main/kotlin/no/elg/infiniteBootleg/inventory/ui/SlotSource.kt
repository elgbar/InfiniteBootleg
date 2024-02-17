package no.elg.infiniteBootleg.inventory.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import no.elg.infiniteBootleg.inventory.container.impl.AutoSortedContainer
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
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

  override fun dragStop(
    event: InputEvent,
    x: Float,
    y: Float,
    pointer: Int,
    payload: Payload?,
    target: DragAndDrop.Target?
  ) {
    val payloadSlot = payload?.getObject() as? InventorySlot ?: return
    if (target != null) {
      val targetSlot = (target.actor.userObject as? InventorySlot) ?: return

      val container = targetSlot.container

      if (targetSlot.index == payloadSlot.index) {
        Main.logger().debug("DAD", "Dragging to same slot, ignoring")
        return
      }

      val targetItem = targetSlot.item
      val draggingItem = payloadSlot.item ?: return
      if (targetItem?.element != draggingItem.element) {
        container.swap(targetSlot.index, payloadSlot.index)
      } else {
        // FIXME This is probably borked :D
        //        val change = targetItem.change(draggingItem.stock.toInt())
        //        container.put(targetSlot.index, draggingItem)
        //        container.remove(payloadSlot.index)
      }
    }
  }

  companion object {
    const val DRAG_ICON_SIZE = Block.BLOCK_SIZE * 3f
  }
}
