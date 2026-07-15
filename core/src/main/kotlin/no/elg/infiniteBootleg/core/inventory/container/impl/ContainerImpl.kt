package no.elg.infiniteBootleg.core.inventory.container.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.events.ContainerEvent
import no.elg.infiniteBootleg.core.events.ItemChangeType
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.inventory.container.Container
import no.elg.infiniteBootleg.core.inventory.container.Container.Companion.NOT_FOUND
import no.elg.infiniteBootleg.core.inventory.container.IndexedItem
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.util.IllegalAction
import no.elg.infiniteBootleg.core.world.ContainerElement
import no.elg.infiniteBootleg.protobuf.ProtoWorld

private val logger = KotlinLogging.logger {}

/**
 * @author kheba
 */
open class ContainerImpl(override val name: String, final override val size: Int = DEFAULT_SIZE) : Container {

  override val type: ProtoWorld.Container.Type get() = ProtoWorld.Container.Type.GENERIC
  override val content: Array<Item?> = arrayOfNulls(size)

  init {
    require(size > 0) { "Inventory size must be greater than zero" }
  }

  override fun indexOfFirstEmpty(): Int = content.indexOfFirst { it == null }
  override fun indexOfFirstNonFull(element: ContainerElement): Int = content.indexOfFirst { it?.element == element && it.stock < it.maxStock }
  override fun indexOfFirst(element: ContainerElement): Int = content.indexOfFirst { it?.element == element }

  override fun indexOfFirst(filter: (Item?) -> Boolean): Int = content.indexOfFirst(filter)

  @Suppress("UNCHECKED_CAST")
  private fun filterElementType(element: ContainerElement): Sequence<Item> =
    // "it" Can not be null because the input element can't be null
    content.asSequence().filter { it?.element == element } as Sequence<Item>

  override fun exists(element: ContainerElement, amount: UInt): Boolean {
    var amountNeeded = amount
    filterElementType(element).forEach { item ->
      amountNeeded -= item.stock
      if (amountNeeded <= 0u) {
        return true
      }
    }
    return false
  }

  override fun count(element: ContainerElement): UInt = filterElementType(element).sumOf(Item::stock)

  override fun add(element: ContainerElement, amount: UInt): UInt {
    if (amount == 0u) return 0u
    var amountNotAdded = amount
    try {
      while (amountNotAdded > 0u) {
        val index = indexOfFirstCanAdd(element)
        if (index < 0) {
          return amountNotAdded
        }
        val contentItem = content[index]
        if (contentItem == null) {
          val toAdd = amountNotAdded.coerceAtMost(Item.DEFAULT_MAX_STOCK)
          amountNotAdded -= toAdd
          content[index] = element.toItem(stock = toAdd)
        } else {
          val canFitInThisItem = contentItem.maxStock - contentItem.stock
          val toAdd = canFitInThisItem.coerceAtMost(amountNotAdded)
          amountNotAdded -= toAdd

          val newItems = contentItem.add(toAdd)
          content[index] = newItems.single()
        }
      }
      return amountNotAdded
    } finally {
      updateContainer(addedItem = element.toItem(UInt.MAX_VALUE, amount - amountNotAdded))
    }
  }

  override fun add(items: List<Item>): List<Item> {
    if (items.isEmpty()) return emptyList()
    val (stateless, stateful) = items.partition { it.element.stateless }
    val collector: MutableMap<ContainerElement, UInt> = HashMap()

    // tally up how many we got of each type
    for (stack in stateless) {
      collector[stack.element] = collector.getOrDefault(stack.element, 0u) + stack.stock
    }

    val notAdded = mutableListOf<Item>()

    // then add them all type by type
    for ((element, stock) in collector) {
      val failedToAdd = add(element, stock)
      // if any elements failed to be added, add them here
      if (failedToAdd > 0u) {
        notAdded += element.toItem(stock = failedToAdd)
      }
    }
    for (item in stateful) {
      val indexOfFirstEmpty = indexOfFirstEmpty()
      if (indexOfFirstEmpty == NOT_FOUND) {
        notAdded += item
      } else {
        set(indexOfFirstEmpty, item)
      }
    }
    return notAdded
  }

  override fun removeAll(element: ContainerElement) {
    var removedStock = 0u
    try {
      for (i in content.indices) {
        val item = content[i]
        if (item?.element == element) {
          removedStock += item.stock
          content[i] = null
        }
      }
    } finally {
      updateContainer(removedItem = element.toItem(maxStock = UInt.MAX_VALUE, stock = removedStock))
    }
  }

  override fun remove(item: Item, amount: UInt): UInt {
    if (amount == 0u) return 0u
    val index = indexOfFirst { it === item }
    if (item.isValid() && index != NOT_FOUND) {
      if (item.canBeUsed(amount)) {
        val newItem = item.remove(amount)
        set(index, newItem)
        return 0u
      } else {
        // we remove more than the item has, so we remove the item and then call the generic remove to remove the rest
        set(index, null)
        return if (item.element.stateless) {
          remove(item.element, amount - item.stock)
        } else {
          amount - item.stock
        }
      }
    } else {
      return remove(item.element, amount)
    }
  }

  override fun remove(element: ContainerElement, amount: UInt, allowStatefulRemoval: Boolean): UInt {
    if (amount == 0u) return 0u
    logger.debug { "Removing $amount of ${element.displayName}" }
    if (!element.stateless) {
      if (allowStatefulRemoval) {
        logger.debug { "Element is stateful, but allowStatefulRemoval=true so it is allowed" }
      } else {
        IllegalAction.STACKTRACE.handle { "Tried to remove a stateful element, this is not allowed without setting allowStatefulRemoval=true in remove" }
        return amount
      }
    }
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
              content[i] = item.remove(stockToRemove)
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
    if ((validOnly && !item.isValid()) || item.stock == 0u) {
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

  override fun set(index: Int, item: Item?) {
    require(index in 0 until size) { "Index out of bounds: $index" }
    require(!(validOnly && item != null && !item.isValid())) { "This container does not allow invalid stacks" }
    val old = content[index]
    content[index] = item
    updateContainer(addedItem = item, removedItem = old)
  }

  override fun swap(index1: Int, index2: Int) {
    require(index1 in 0 until size) { "Index 1 out of bounds: $index1" }
    require(index2 in 0 until size) { "Index 2 out of bounds: $index2" }
    val item1 = content[index1]
    val item2 = content[index2]
    if (item1 != null || item2 != null) {
      content[index1] = item2
      content[index2] = item1
      updateContainer(changeType = null)
    }
  }

  private fun updateContainer(addedItem: Item? = null, removedItem: Item? = null) {
    if (addedItem == removedItem && addedItem != null && addedItem.equalsIncludingStock(removedItem)) {
      // Do not send an update when the items are the same
      return
    }
    updateContainer(ItemChangeType.getItemChangeType(addedItem, removedItem))
  }

  protected open fun updateContainer(changeType: ItemChangeType?) {
    EventManager.dispatchEvent(ContainerEvent.ContentChanged(this, changeType = changeType))
  }

  override fun iterator(): MutableIterator<IndexedItem> {
    return object : MutableIterator<IndexedItem> {
      var index: Int = -1

      override fun hasNext(): Boolean = index < size - 1

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

  override fun toString(): String = "ContainerImpl(name='$name', size=$size, type=$type)"

  companion object {
    const val DEFAULT_SIZE = 40
  }
}
