package no.elg.infiniteBootleg.core.inventory.container.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.events.ContainerEvent
import no.elg.infiniteBootleg.core.events.ItemChangeType
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.inventory.container.Container
import no.elg.infiniteBootleg.core.inventory.container.Container.Companion.NOT_FOUND
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

  override fun indexOfFirstEmpty(): Int = indexOfFirst { it == null }

  /**
   * @param element The element to match against
   * @return The index of the first element of type `element` and where the stock is less than max stock, or [NOT_FOUND] if either none is found or the [element] is not [ContainerElement.stateless]
   */
  private fun indexOfFirstNonFull(element: ContainerElement): Int =
    if (element.stateless) {
      content.indexOfFirst { it?.element == element && it.stock < it.maxStock }
    } else {
      NOT_FOUND
    }

  /**
   * @param filter The filter to match against
   * @return The index of the first slot that matches the given filter, or [NOT_FOUND] if the container does not contain such item
   */
  private fun indexOfFirst(filter: (Item?) -> Boolean): Int = content.indexOfFirst(filter)

  /**
   * @param element The element to match against
   * @return The index of in the container where the [element] can be added, or [NOT_FOUND] if there is no slot to add the element to
   */
  private fun indexOfFirstCanAdd(element: ContainerElement): Int = indexOfFirstNonFull(element).let { if (it == NOT_FOUND) indexOfFirstEmpty() else it }

  @Suppress("UNCHECKED_CAST")
  private fun filterElementType(element: ContainerElement): Sequence<Item> =
    if (element.canBeHandled) {
      // "it" Can not be null because the input element can't be null
      content.asSequence().filter { it?.element == element } as Sequence<Item>
    } else {
      emptySequence()
    }

  override fun exists(element: ContainerElement, amount: UInt): Boolean {
    if (element.isAlwaysPresent) {
      return true
    } else if (element.canBeHandled) {
      var amountNeeded = amount
      filterElementType(element).forEach { item ->
        amountNeeded -= item.stock
        if (amountNeeded <= 0u) {
          return true
        }
      }
    }
    return false
  }

  override fun count(element: ContainerElement): UInt =
    when {
      element.isAlwaysPresent -> UInt.MAX_VALUE
      element.canBeHandled -> filterElementType(element).sumOf(Item::stock)
      else -> 0u // If it cant be handled we cant have it in the inventory
    }

  override fun add(element: ContainerElement, amount: UInt): UInt {
    if (isNoopAmountOrElement(element, amount)) return 0u
    require(!validOnly || element.canBeHandled) { "This container does not allow invalid elements" }
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

  override fun add(items: Iterable<Item>): List<Item> {
    val filteredItems = items.filter(::isItemValid)
    if (filteredItems.isEmpty()) return emptyList()
    val (stateless, stateful) = filteredItems.partition { it.element.stateless }
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
    if (element.isAlwaysPresent) {
      return
    }
    // Note: element.canBeHandled is not checked as this is a corrective action
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
    if (isNoopAmountOrElement(item.element, amount)) return 0u
    require(isItemValid(item)) { "This container does not allow invalid items" }
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
    if (isNoopAmountOrElement(element, amount)) return 0u
    // Note: element.canBeHandled is not checked as this is a corrective action
    logger.debug { "Removing $amount of ${element.displayName}" }
    if (checkAllowStatefulRemoval(element, allowStatefulRemoval)) {
      return amount
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

  override fun removeAll(item: Item) {
    if (isItemValid(item) || isNoopAmountOrElement(item)) {
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
    requireValidIndex(index)
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

  override fun contains(item: Item?): Boolean = item != null && content.contains(item)

  override fun get(index: Int): Item? {
    requireValidIndex(index)
    return content[index]
  }

  override fun set(index: Int, item: Item?) {
    requireValidIndex(index)
    require(isItemValid(item, nullItemHasValidity = true)) { "This container does not allow invalid items" }
    val old = content[index]
    content[index] = if (item != null && item.element.canBeHandled) item else null
    updateContainer(addedItem = item, removedItem = old)
  }

  override fun swap(index1: Int, index2: Int) {
    requireValidIndex(index1)
    requireValidIndex(index2)
    val item1 = content[index1]
    val item2 = content[index2]
    if (item1 != null || item2 != null) {
      content[index1] = item2
      content[index2] = item1
      updateContainer(changeType = null)
    }
  }

  private fun updateContainer(addedItem: Item? = null, removedItem: Item? = null) {
    if ((addedItem == removedItem && addedItem != null && addedItem.equalsIncludingStock(removedItem)) || ((addedItem?.stock ?: 0u) == 0u && (removedItem?.stock ?: 0u) == 0u)) {
      // Do not send an update when the items are the same or there are no changed items
      return
    }
    updateContainer(ItemChangeType.getItemChangeType(addedItem, removedItem))
  }

  protected open fun updateContainer(changeType: ItemChangeType?) {
    EventManager.dispatchEvent(ContainerEvent.ContentChanged(this, changeType = changeType))
  }

  override fun iterator(): MutableIterator<Item?> {
    return object : MutableIterator<Item?> {
      var index: Int = -1

      override fun hasNext(): Boolean = index < size - 1

      override fun next(): Item? {
        val nextIndex = ++index
        return content[nextIndex]
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

  fun isNoopAmountOrElement(element: ContainerElement, amount: UInt): Boolean = amount == 0u || element.isAlwaysPresent
  fun isNoopAmountOrElement(item: Item): Boolean = isNoopAmountOrElement(item.element, item.stock)

  private fun checkAllowStatefulRemoval(element: ContainerElement, allowStatefulRemoval: Boolean): Boolean {
    if (!element.stateless) {
      if (allowStatefulRemoval) {
        logger.debug { "Element is stateful, but allowStatefulRemoval=true so it is allowed" }
      } else {
        IllegalAction.STACKTRACE.handle { "Tried to remove a stateful element, this is not allowed without setting allowStatefulRemoval=true in remove" }
        return true
      }
    }
    return false
  }

  fun isItemValid(item: Item): Boolean = !validOnly || item.isValid()
  fun isItemValid(item: Item?, nullItemHasValidity: Boolean): Boolean = !validOnly || (item?.isValid() ?: nullItemHasValidity)
  fun requireValidIndex(index: Int) = require(index in 0..<size) { "Index out of bounds: $index. bounds are 0 ..< $size" }

  companion object {
    const val DEFAULT_SIZE = 40
  }
}
