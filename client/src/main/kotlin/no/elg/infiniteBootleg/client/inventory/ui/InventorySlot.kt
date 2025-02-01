package no.elg.infiniteBootleg.client.inventory.ui

import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.items.Item

data class InventorySlot(val ownedContainer: OwnedContainer, val index: Int) {
  val item: Item? get() = ownedContainer.container[index]

  val isEmpty: Boolean
    get() = item == null
}
