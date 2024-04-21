package no.elg.infiniteBootleg.inventory.ui

import no.elg.infiniteBootleg.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.items.Item

data class InventorySlot(val ownedContainer: OwnedContainer, val index: Int) {
  val item: Item? get() = ownedContainer.container[index]

  val isEmpty: Boolean
    get() = item == null
}
