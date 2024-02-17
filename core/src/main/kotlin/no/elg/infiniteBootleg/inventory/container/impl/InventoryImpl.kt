package no.elg.infiniteBootleg.inventory.container.impl

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.scenes.scene2d.Actor
import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.Container.Companion.close
import no.elg.infiniteBootleg.inventory.container.Container.Companion.open
import no.elg.infiniteBootleg.inventory.container.Inventory
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.clientWorld
import java.util.concurrent.CompletableFuture

/**
 * An abstracted storage that handles what item is selected
 *
 * @author kheba
 */
class InventoryImpl(override val holder: Entity, override val container: Container) : Inventory {
  override var selected: Int = 0
    private set

  override val isOpen: Boolean get() = containerActor.let { it?.isDone == true && it.get().isVisible }

  override fun next() {
    selected = (selected + 1) % this.size
  }

  override fun prev() {
    if (selected == 0) {
      selected = size - 1
      return
    }
    selected = (selected - 1) % this.size
  }

  private val containerActor: CompletableFuture<Actor>?
    get() = holder.clientWorld?.render?.getContainerActor(container)

  override fun open() {
    container.open(holder)
  }

  override fun close() {
    container.close(holder)
  }

  override fun select(index: Int) {
    selected = Preconditions.checkPositionIndex(index, size - 1)
  }

  override fun holding(): Item? = container[selected]

  override val size: Int
    get() = container.size
}
