package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.items.Item

sealed interface ItemChangeType {
  val oldItem: Item?
  val newItem: Item?

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

data class ItemAddedChangeType(override val newItem: Item) : ItemChangeType {
  override val oldItem: Item? get() = null
}

data class ItemRemovedChangeType(override val oldItem: Item) : ItemChangeType {
  override val newItem: Item? get() = null
}

/**
 * The item was swapped out with something else, typically the material of the item changed.
 *
 * Note that if the [Item.maxStock] changes this will be considered a [ItemChangedChangeType]
 */
data class ItemChangedChangeType(override val oldItem: Item, override val newItem: Item) : ItemChangeType {
  init {
    require(oldItem != newItem) { "Old and new items does not differ, use ItemStockChangeType" }
  }
}

/**
 * The stock of an item updated, but the item itself is the same
 *
 * Note that if the [Item.maxStock] changes this will be considered a [ItemChangedChangeType]
 */
data class ItemStockChangeType(override val oldItem: Item, override val newItem: Item) : ItemChangeType {
  init {
    require(oldItem == newItem && !oldItem.equalsIncludingStock(newItem)) { "Old and new items differ in other ways than the stock, $oldItem and $newItem" }
  }
}

///**
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
//data class ItemSwappedChangeType(
//  override val oldItem: Item?,
//  override val newItem: Item?,
//  val oldItemOther: Item?,
//  val newItemOther: Item?
//) : ItemChangeType{
//  init {
//    require(oldItem != null || newItem != null) { "Either the old or the new item must be non-null" }
//    require(oldItem === newItemOther) { "The items at index and indexOther must be swapped" }
//    require(newItem === oldItemOther) { "The items at index and indexOther must be swapped" }
//  }
//}
