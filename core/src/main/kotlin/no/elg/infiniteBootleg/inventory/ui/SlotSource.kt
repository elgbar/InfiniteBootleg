package no.elg.infiniteBootleg.inventory.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload
import no.elg.infiniteBootleg.inventory.container.impl.AutoSortedContainer

/**
 * @author Daniel Holderbaum
 */
class SlotSource(actor: SlotActor) : DragAndDrop.Source(actor) {
  private val sourceSlot: Slot = actor.slot

  override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): Payload? {
    val srcTs = sourceSlot.stack ?: return null

    if (srcTs.stock == 0u || sourceSlot.containerActor.container is AutoSortedContainer) {
      return null
    }

    val payload = Payload()
    val payloadSlot = Slot(sourceSlot)

    //        srcTs.take(srcTs.getStock());
    payload.setObject(payloadSlot)

    val icon = srcTs.element.textureRegion ?: return null

    val image = Image(icon.textureRegion)
    val dragActor: Actor = image
    payload.dragActor = dragActor

    val validDragActor: Actor = image
    // validDragActor.setColor(0, 1, 0, 1);
    payload.validDragActor = validDragActor

    val invalidDragActor: Actor = image
    // invalidDragActor.setColor(1, 0, 0, 1);
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
    val payloadSlot = payload?.getObject() as? Slot ?: return
    if (target != null) {
      val targetSlot = (target.actor as? SlotActor)?.slot ?: return

      val container = targetSlot.containerActor.container

      val targetItem = targetSlot.stack
      val draggingSlot = payloadSlot.stack ?: return
      if (targetItem == null) {
        container.put(targetSlot.index, draggingSlot)
        container.remove(payloadSlot.index)
      } else if (targetItem.element == draggingSlot.element) {
        // FIXME This is probably borked :D
        val change = targetItem.change(draggingSlot.stock.toInt())
        container.put(targetSlot.index, draggingSlot)
        container.remove(payloadSlot.index)
      } else {
        // swap the two items
        val targetTs = container[targetSlot.index]
        val payloadTs = container[payloadSlot.index]

        container.put(targetSlot.index, payloadTs)
        container.put(payloadSlot.index, targetTs)
      }
    }
    //        else {
    //            this.sourceSlot.add(payloadSlot.getItem(), payloadSlot.getStock());
    //        }
  }
}
