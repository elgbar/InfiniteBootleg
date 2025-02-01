package no.elg.infiniteBootleg.core.items

import no.elg.infiniteBootleg.core.world.Tool

/**
 * Represent something a player may possess or something that may be in the world
 *
 * @property maxStock The maximum charge of this item
 * @property stock The current charge of this item, **not included in the equals method**
 */
data class ToolItem(
  override val element: Tool,
  override val maxStock: UInt = Item.Companion.DEFAULT_MAX_STOCK,
  override val stock: UInt = Item.Companion.DEFAULT_MAX_STOCK
) : Item {

  override val itemType: ItemType = ItemType.TOOL

  /**
   * Change the charge of this item by [usages] amount
   *
   * @return The resulting item, or `null` if the item would be depleted
   */
  override fun use(usages: UInt): ToolItem? {
    if (willBeDepleted(usages)) return null
    return ToolItem(element, maxStock, stock - usages)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ToolItem) return false

    if (element != other.element) return false
    return maxStock == other.maxStock
  }

  override fun hashCode(): Int {
    var result = element.hashCode()
    result = 31 * result + maxStock.hashCode()
    return result
  }
}
