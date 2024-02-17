package no.elg.infiniteBootleg.inventory.ui

import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.items.Item

data class InventorySlot(val container: Container, val index: Int) {
  val item: Item? get() = container[index]

  val isEmpty: Boolean
    get() = item == null
}
