package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.events.ItemStockChangeType.Companion.calculateStockChange
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.util.IllegalAction

sealed interface ItemChangeType {
  /**
   * The item that was removed, or `null` if the item was added
   */
  val removedItem: Item?

  /**
   * The item that was added, or `null` if the item was removed
   */
  val addedItem: Item?

  companion object {
    fun getItemChangeType(addedItem: Item?, removedItem: Item?): ItemChangeType? {
      return when {
        addedItem != null && removedItem != null ->
          if (addedItem == removedItem) {
            ItemStockChangeType.calculateStockChange(removedItem, addedItem)
          } else {
            ItemChangedChangeType(removedItem, addedItem)
          }

        addedItem != null -> ItemAddedChangeType(addedItem)
        removedItem != null -> ItemRemovedChangeType(removedItem)
        else -> null // unknown change
      }
    }
  }
}

data class ItemAddedChangeType(override val addedItem: Item) : ItemChangeType {
  override val removedItem: Item? get() = null
}

data class ItemRemovedChangeType(override val removedItem: Item) : ItemChangeType {
  override val addedItem: Item? get() = null
}

/**
 * The item was swapped out with something else, typically the material of the item changed.
 *
 * Note that if the [Item.maxStock] changes this will be considered a [ItemChangedChangeType]
 */
data class ItemChangedChangeType(override val removedItem: Item, override val addedItem: Item) : ItemChangeType {
  init {
    require(removedItem != addedItem) { "Old and new items does not differ, use ItemStockChangeType" }
  }
}

/**
 * The stock of an item updated, but the item itself is the same.
 * Only one of the items will be set, the other will be null.
 *
 * Use [calculateStockChange] to calculate the stock change.
 *
 * Note that if the [Item.maxStock] changes this will be considered a [ItemChangedChangeType]
 */
class ItemStockChangeType private constructor(override val removedItem: Item?, override val addedItem: Item?) : ItemChangeType {
  init {
    require(removedItem != null || addedItem != null) { "Old and new items cannot be null" }
    require(removedItem == null || addedItem == null) { "Old and new items cannot both be set" }
  }

  companion object {
    /**
     * Calculate how the two items changed in stock.
     *
     * This requires that they are equal except for the stock
     */
    fun calculateStockChange(removedItem: Item, addedItem: Item): ItemStockChangeType? {
      require(removedItem == addedItem) { "Old and new items differ in other ways than the stock, $removedItem and $addedItem" }
      val addedStock = addedItem.stock
      val removedStock = removedItem.stock
      return if (addedStock > removedStock) {
        ItemStockChangeType(null, addedItem.remove(removedStock))
      } else if (addedStock < removedStock) {
        ItemStockChangeType(removedItem.remove(addedStock), null)
      } else {
        IllegalAction.STACKTRACE.handle { "Calculating stock change when the stock is the same: removed $removedItem, added $addedItem" }
        // No stock difference
        null
      }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ItemStockChangeType) return false

    if (removedItem != other.removedItem) return false
    if (addedItem != other.addedItem) return false

    return true
  }

  override fun hashCode(): Int {
    var result = removedItem?.hashCode() ?: 0
    result = 31 * result + (addedItem?.hashCode() ?: 0)
    return result
  }
}
