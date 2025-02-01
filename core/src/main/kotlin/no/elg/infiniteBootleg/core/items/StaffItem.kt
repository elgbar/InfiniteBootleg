package no.elg.infiniteBootleg.core.items

import no.elg.infiniteBootleg.core.world.Staff

data class StaffItem(
  override val element: Staff,
  override val maxStock: UInt,
  override val stock: UInt
) : Item {

  override val itemType: ItemType = ItemType.TOOL

  override fun use(usages: UInt): StaffItem? {
    if (willBeDepleted(usages)) return null
    return StaffItem(element, maxStock, stock - usages)
  }
}
