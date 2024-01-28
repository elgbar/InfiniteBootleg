package no.elg.infiniteBootleg.inventory.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload

/**
 * @author Daniel Holderbaum
 */
class SlotTarget(actor: SlotActor) : DragAndDrop.Target(actor) {
  private val targetSlot: Slot = actor.slot

  init {
    getActor().color = Color.LIGHT_GRAY
  }

  override fun drag(
    source: DragAndDrop.Source,
    payload: Payload,
    x: Float,
    y: Float,
    pointer: Int
  ): Boolean {
    val payloadSlot = payload.getObject() as Slot
    // if (targetSlot.getItem() == payloadSlot.getItem() ||
    // targetSlot.getItem() == null) {
    actor.color = Color.WHITE
    return true
    // } else {
    // getActor().setColor(Color.DARK_GRAY);
    // return false;
    // }
  }

  override fun drop(
    source: DragAndDrop.Source,
    payload: Payload,
    x: Float,
    y: Float,
    pointer: Int
  ) {
  }

  override fun reset(source: DragAndDrop.Source, payload: Payload) {
    actor.color = Color.LIGHT_GRAY
  }
}
