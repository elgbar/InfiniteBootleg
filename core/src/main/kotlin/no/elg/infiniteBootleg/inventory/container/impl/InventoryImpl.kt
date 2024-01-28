package no.elg.infiniteBootleg.inventory.container.impl

import com.badlogic.ashley.core.Entity
import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.Inventory
import no.elg.infiniteBootleg.inventory.ui.ContainerActor
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.clientWorld

/**
 * An abstracted storage that handles what item is selected
 *
 * @author kheba
 */
class InventoryImpl(override val holder: Entity, override val container: Container) : Inventory {
  override var sel: Int = 0
    private set

  override val isOpen: Boolean get() = containerActor?.isVisible ?: false

  override fun next() {
    sel = (sel + 1) % this.size
  }

  override fun prev() {
    if (sel == 0) {
      sel = size - 1
      return
    }
    sel = (sel - 1) % this.size
  }

  private val containerActor: ContainerActor?
    get() = holder.clientWorld?.render?.getContainerActor(container)

  override fun open() {
    val actor = containerActor
    if (actor != null) {
      actor.isVisible = true
    }
  }

  override fun close() {
    val actor = containerActor
    if (actor != null) {
      actor.isVisible = false
    }
  }

  override fun select(index: Int) {
    sel = Preconditions.checkPositionIndex(index, size - 1)
  }

  override fun holding(): Item? {
    return container[sel]
  }

  override val size: Int
    get() = container.size
}
