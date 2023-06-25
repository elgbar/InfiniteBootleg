package no.elg.infiniteBootleg.items

import no.elg.infiniteBootleg.world.Material

/**
 * Represent something a player may possess or something that may be in the world
 *
 * @property material The material of this item
 * @property maxCharge The maximum charge of this item
 * @property charge The current charge of this item, **not included in the equals method**
 */
data class Item(
  val material: Material,
  val maxCharge: UInt = UInt.MAX_VALUE,
  val charge: UInt = maxCharge
) {

  /**
   * Change the charge of this item by [usages] amount
   *
   * @return The resulting item, or `null` if the item would be depleted
   */
  fun use(usages: UInt = 1u): Item? {
    if (usages >= charge) return null
    return Item(material, maxCharge)
  }

  fun canBeUsed(usages: UInt = 1u): Boolean = usages <= charge

  fun equalsIncludingCharge(other: Any?): Boolean = equals(other) && charge == (other as Item).charge

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Item) return false

    if (material != other.material) return false
    return maxCharge == other.maxCharge
  }

  override fun hashCode(): Int {
    var result = material.hashCode()
    result = 31 * result + maxCharge.hashCode()
    return result
  }
}