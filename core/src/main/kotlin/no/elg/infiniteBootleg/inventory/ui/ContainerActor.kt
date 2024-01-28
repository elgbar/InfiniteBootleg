package no.elg.infiniteBootleg.inventory.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.kotcrab.vis.ui.widget.VisWindow
import no.elg.infiniteBootleg.inventory.container.Container

/**
 * @author kheba //TODO resize to a proper size //TODO display how many there is in each stack
 */
class ContainerActor(val container: Container, dragAndDrop: DragAndDrop) : VisWindow(container.name) {
  private val slots: Array<SlotActor?>
  private var update = false

  init {
    isResizable = false
    isMovable = true

    slots = arrayOfNulls(container.size)

    addCloseButton()

    defaults().space(3f)
    row().fill().expandX()

    var i = 0
    for (cslot in container) {
      val slotActor =
        SlotActor(skin, Slot(this, cslot.content, cslot.index), container)

      slots[cslot.index] = slotActor
      dragAndDrop.addSource(SlotSource(slotActor))
      dragAndDrop.addTarget(SlotTarget(slotActor))

      add(slotActor)

      i++
      if (i % 10 == 0) {
        row()
      }
    }

    pack()
    setPosition(
      (Gdx.graphics.width - width) / 2f,
      (Gdx.graphics.height - height) / 2f
    )

    // Hidden by default
    isVisible = false
  }

  override fun act(delta: Float) {
    super.act(delta)
    if (update) {
      for (containerSlot in container) {
        slots[containerSlot.index]?.update()
      }
      update = false
    }
  }

  fun update() {
    update = true
  }
}
