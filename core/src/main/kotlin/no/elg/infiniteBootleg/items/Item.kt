package no.elg.infiniteBootleg.items

import no.elg.infiniteBootleg.world.Material

/**
 * Represent something a player may possess or something that may be in the world
 *
 * @property material The material of this item
 * @property maxStock The maximum charge of this item
 * @property stock The current charge of this item, **not included in the equals method**
 */
data class Item(
  val material: Material,
  val maxStock: UInt = UInt.MAX_VALUE,
  val stock: UInt = maxStock
) {

  /**
   * Change the charge of this item by [usages] amount
   *
   * @return The resulting item, or `null` if the item would be depleted
   */
  fun use(usages: UInt = 1u): Item? {
    if (usages > stock) return null
    return Item(material, maxStock, stock - usages)
  }

  fun canBeUsed(usages: UInt = 1u): Boolean = usages <= stock

  fun equalsIncludingCharge(other: Any?): Boolean = equals(other) && stock == (other as Item).stock

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Item) return false

    if (material != other.material) return false
    return maxStock == other.maxStock
  }

  override fun hashCode(): Int {
    var result = material.hashCode()
    result = 31 * result + maxStock.hashCode()
    return result
  }
}
