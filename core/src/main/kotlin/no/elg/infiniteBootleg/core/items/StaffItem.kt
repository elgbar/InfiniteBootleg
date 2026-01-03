package no.elg.infiniteBootleg.core.items

import no.elg.infiniteBootleg.core.world.Staff
import no.elg.infiniteBootleg.core.world.magic.Description

data class StaffItem(override val element: Staff, override val maxStock: UInt, override val stock: UInt) :
  Item,
  Description {

  override val itemType: ItemType get() = ItemType.TOOL

  override fun remove(usages: UInt): StaffItem? {
    if (willBeDepleted(usages)) return null
    return copy(stock = stock - usages)
  }

  override val description: String
    get() = "A staff made of ${element.wood.type.displayName} wood, adorned with ${element.gems.size} gem(s) and ${element.rings.size} ring(s).\n${element.description}"
}
