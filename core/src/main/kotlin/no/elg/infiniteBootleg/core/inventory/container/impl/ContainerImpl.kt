package no.elg.infiniteBootleg.core.inventory.container.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.events.ContainerEvent
import no.elg.infiniteBootleg.core.events.ItemChangeType
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.inventory.container.Container
import no.elg.infiniteBootleg.core.inventory.container.IndexedItem
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.world.ContainerElement
import no.elg.infiniteBootleg.protobuf.ProtoWorld

private val logger = KotlinLogging.logger {}

/**
 * @author kheba
 */
open class ContainerImpl(
  override val name: String,
  final override val size: Int = DEFAULT_SIZE
) : Container {

  override val type: ProtoWorld.Container.Type get() = ProtoWorld.Container.Type.GENERIC
  override val content: Array<Item?> = arrayOfNulls(size)

  init {
    require(size > 0) { "Inventory size must be greater than zero" }
  }

  override fun indexOfFirstEmpty(): Int = content.indexOfFirst { it == null }
  override fun indexOfFirstNonFull(element: ContainerElement): Int = content.indexOfFirst { it?.element == element && it.stock < it.maxStock }
  override fun indexOfFirst(element: ContainerElement): Int = content.indexOfFirst { it?.element == element }

  override fun indexOfFirst(filter: (Item?) -> Boolean): Int = content.indexOfFirst(filter)

  override fun add(element: ContainerElement, amount: UInt): UInt {
    if (amount == 0u) return 0u
    var amountNotAdded = amount
    try {
      while (amountNotAdded > 0u) {
        val index = indexOfFirstNonFull(element).let { if (it < 0) indexOfFirstEmpty() else it }
        if (index < 0) {
          return amountNotAdded
        }
        val existingItem = content[index] ?: element.toItem(stock = 0u)
        val canFitInThisItem = Item.Companion.DEFAULT_MAX_STOCK - existingItem.stock
        val toAdd = canFitInThisItem.coerceAtMost(amountNotAdded)
        amountNotAdded -= toAdd

        val newItem = existingItem.change(toAdd.toInt()).single()
        content[index] = newItem
      }
      return amountNotAdded
    } finally {
      updateContainer(addedItem = element.toItem(UInt.MAX_VALUE, amount - amountNotAdded))
    }
  }

  override fun removeAll(element: ContainerElement) = remove(element.toItem(UInt.MAX_VALUE, UInt.MAX_VALUE))

  override fun remove(element: Item, amount: UInt): UInt = remove(element.element, amount)

  override fun remove(element: ContainerElement, amount: UInt): UInt {
    if (amount == 0u) return 0u
    logger.debug { "Removing $amount of $element" }
    var stockToRemove = amount
    var i = 0
    val length = content.size
    try {
      while (i < length) {
        val item = content[i]
        if (item != null && element === item.element) {
          val newAmount = (item.stock - stockToRemove).toInt()
          if (newAmount < 0) {
            stockToRemove -= item.stock
            content[i] = null
          } else {
            if (newAmount != 0) {
              content[i] = item.use(stockToRemove)
            } else {
              content[i] = null
            }
            stockToRemove = 0u
            break
          }
        }
        i++
      }
      return stockToRemove
    } finally {
      updateContainer(removedItem = element.toItem(UInt.MAX_VALUE, amount - stockToRemove))
    }
  }

  override fun remove(item: Item) {
    if (validOnly && !item.isValid() || item.stock == 0u) {
      return
    }
    var i = 0
    val length = content.size
    var stockRemoved = 0u
    while (i < length) {
      if (item == content[i]) {
        stockRemoved += item.stock
        content[i] = null
      }
      i++
    }
    updateContainer(removedItem = item.copyToFit(stockRemoved))
  }

  override fun remove(index: Int) {
    val old = content[index]
    if (old != null) {
      content[index] = null
      updateContainer(removedItem = old)
    }
  }

  override fun clear() {
    for (i in content.indices) {
      content[i] = null
    }
    updateContainer(changeType = null)
  }

  override fun contains(item: Item?): Boolean {
    if (item == null || (validOnly && !item.isValid())) {
      return false
    }
    for (slot in this) {
      if (item == slot.content) {
        return true
      }
    }
    return false
  }

  override fun get(index: Int): Item? {
    require(index in 0 until size) { "Index out of bounds: $index" }
    return content[index]
  }

  override fun put(index: Int, item: Item?) {
    require(index in 0 until size) { "Index out of bounds: $index" }
    require(!(validOnly && item != null && !item.isValid())) { "This container does not allow invalid stacks" }
    val old = content[index]
    content[index] = item
    updateContainer(addedItem = item, removedItem = old)
  }

  override fun swap(index1: Int, index2: Int) {
    require(index1 in 0 until size) { "Index out of bounds: $index1" }
    require(index2 in 0 until size) { "Index out of bounds: $index2" }
    val item1 = content[index1]
    val item2 = content[index2]
    if (item1 != null || item2 != null) {
      content[index1] = item2
      content[index2] = item1
//      val changeType = ItemSwappedChangeType(
//        index1,
//        oldItem = item1,
//        newItem = item2,
//        indexOther = index2,
//        oldItemOther = item2,
//        newItemOther = item1,
//      )
      updateContainer(changeType = null)
    }
  }

  private fun updateContainer(addedItem: Item? = null, removedItem: Item? = null) {
    updateContainer(ItemChangeType.getItemChangeType(addedItem, removedItem))
  }

  protected open fun updateContainer(changeType: ItemChangeType?) {
    EventManager.dispatchEvent(ContainerEvent.ContentChanged(this, changeType = changeType))
  }

  override fun iterator(): MutableIterator<IndexedItem> {
    return object : MutableIterator<IndexedItem> {
      var index: Int = -1

      override fun hasNext(): Boolean = index < size

      override fun next(): IndexedItem {
        val nextIndex = ++index
        return IndexedItem(nextIndex, content[nextIndex])
      }

      override fun remove() {
        check(index >= 0) { "Next has not been called yet" }
        remove(index)
      }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ContainerImpl) return false

    if (name != other.name) return false
    if (size != other.size) return false
    if (!content.contentEquals(other.content)) return false
    if (type != other.type) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + size
    result = 31 * result + content.contentHashCode()
    result = 31 * result + type.hashCode()
    return result
  }

  override fun toString(): String {
    return "ContainerImpl(name='$name', size=$size, type=$type)"
  }

  companion object {
    const val DEFAULT_SIZE = 40
  }
}
