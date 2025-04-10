package no.elg.infiniteBootleg.client.inventory.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload

class SlotTarget(actor: Actor) : DragAndDrop.Target(actor) {

  override fun drag(
    source: DragAndDrop.Source,
    payload: Payload,
    x: Float,
    y: Float,
    pointer: Int
  ): Boolean = true

  override fun drop(
    source: DragAndDrop.Source,
    payload: Payload,
    x: Float,
    y: Float,
    pointer: Int
  ) {
  }
}
