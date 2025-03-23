package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.items.Item

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
            ItemStockChangeType(removedItem, addedItem)
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
 * The stock of an item updated, but the item itself is the same
 *
 * Note that if the [Item.maxStock] changes this will be considered a [ItemChangedChangeType]
 */
data class ItemStockChangeType(override val removedItem: Item, override val addedItem: Item) : ItemChangeType {
  init {
    require(removedItem == addedItem && !removedItem.equalsIncludingStock(addedItem)) { "Old and new items differ in other ways than the stock, $removedItem and $addedItem" }
  }
}

// /**
// * Items in [index] and [indexOther] were swapped.
// *
// * Either [oldItem]/[oldItemOther] or [newItem]/[newItemOther] of will be non-null
// *
// * @param index The index of the first item
// * @param oldItem The old item at [index]
// * @param newItem The new item at [index]
// * @param indexOther The index of the second item
// * @param oldItemOther The old item at [indexOther]
// * @param newItemOther The new item at [indexOther]
// */
// data class ItemSwappedChangeType(
//  override val oldItem: Item?,
//  override val newItem: Item?,
//  val oldItemOther: Item?,
//  val newItemOther: Item?
// ) : ItemChangeType{
//  init {
//    require(oldItem != null || newItem != null) { "Either the old or the new item must be non-null" }
//    require(oldItem === newItemOther) { "The items at index and indexOther must be swapped" }
//    require(newItem === oldItemOther) { "The items at index and indexOther must be swapped" }
//  }
// }
