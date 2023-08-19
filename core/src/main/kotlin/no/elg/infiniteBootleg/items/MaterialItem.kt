package no.elg.infiniteBootleg.items

import no.elg.infiniteBootleg.items.Item.Companion.DEFAULT_MAX_STOCK
import no.elg.infiniteBootleg.world.Material

/**
 * Represent something a player may possess or something that may be in the world
 *
 * @property element The material of this item
 * @property maxStock The maximum charge of this item
 * @property stock The current charge of this item, **not included in the equals method**
 */
data class MaterialItem(
  override val element: Material,
  override val maxStock: UInt = DEFAULT_MAX_STOCK,
  override val stock: UInt = DEFAULT_MAX_STOCK
) : Item {

  override val itemType: ItemType = ItemType.BLOCK

  /**
   * Change the charge of this item by [usages] amount
   *
   * @return The resulting item, or `null` if the item would be depleted
   */
  override fun use(usages: UInt): MaterialItem? {
    if (willBeDepleted(usages)) return null
    return MaterialItem(element, maxStock, stock - usages)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MaterialItem) return false

    if (element != other.element) return false
    return maxStock == other.maxStock
  }

  override fun hashCode(): Int {
    var result = element.hashCode()
    result = 31 * result + maxStock.hashCode()
    return result
  }
}
