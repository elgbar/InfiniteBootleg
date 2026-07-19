package no.elg.infiniteBootleg.core.items

import no.elg.infiniteBootleg.core.world.FistToolData
import no.elg.infiniteBootleg.core.world.Tool
import no.elg.infiniteBootleg.core.world.ToolData

/**
 * Represent a tool the player can use on blocks in the world
 *
 * @property maxStock The maximum charge of this item
 * @property stock The current charge of this item, **not included in the equals method**
 */
data class ToolItemImpl<DATA : ToolData>(
  override val element: Tool<DATA>,
  override val maxStock: UInt = Item.DEFAULT_MAX_STOCK,
  override val stock: UInt = Item.DEFAULT_MAX_STOCK,
  override val data: DATA
) : ToolItem<DATA> {

  init {
    require(data != FistToolData) { "use FistItem for Fist" }
  }

  /**
   * Change the charge of this item by [usages] amount
   *
   * @return The resulting item, or `null` if the item would be depleted
   */
  override fun remove(usages: UInt): ToolItem<DATA>? {
    if (willBeDepleted(usages)) return null
    return copy(stock = stock - usages)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ToolItem<*>) return false

    if (element != other.element) return false
    if (data != other.data) return false
    return maxStock == other.maxStock
  }

  override fun hashCode(): Int {
    var result = element.hashCode()
    result = 31 * result + maxStock.hashCode()
    result = 31 * result + data.hashCode()
    return result
  }
}
