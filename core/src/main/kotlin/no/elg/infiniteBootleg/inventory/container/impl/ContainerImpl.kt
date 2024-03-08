package no.elg.infiniteBootleg.inventory.container.impl

import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.events.ContainerEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.IndexedItem
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ContainerElement

/**
 * @author kheba
 */
open class ContainerImpl(
  final override val size: Int,
  override var name: String = "Container"
) : Container {

  override val content: Array<Item?> = arrayOfNulls(size)
  override val type: ProtoWorld.Container.Type get() = ProtoWorld.Container.Type.GENERIC

  init {
    Preconditions.checkArgument(size > 0, "Inventory size must be greater than zero")
  }

  override fun indexOfFirstEmpty(): Int = content.indexOfFirst { it == null }
  override fun indexOfFirstNonFull(element: ContainerElement): Int = content.indexOfFirst { it?.element == element && it.stock < it.maxStock }
  override fun indexOfFirst(element: ContainerElement): Int = content.indexOfFirst { it?.element == element }

  override fun indexOfFirst(filter: (Item?) -> Boolean): Int = content.indexOfFirst(filter)

  override fun add(element: ContainerElement, amount: UInt): UInt {
    if (amount == 0u) return 0u
    try {
      var amountNotAdded = amount
      while (amountNotAdded > 0u) {
        val index = indexOfFirstNonFull(element).let { if (it < 0) indexOfFirstEmpty() else it }
        if (index < 0) {
          return amountNotAdded
        }
        val existingItem = content[index] ?: element.toItem(stock = 0u)
        val canFitInThisItem = Item.DEFAULT_MAX_STOCK - existingItem.stock
        val toAdd = canFitInThisItem.coerceAtMost(amountNotAdded)
        amountNotAdded -= toAdd

        val newItem = existingItem.change(toAdd.toInt()).single()
        content[index] = newItem
      }
      return amountNotAdded
    } finally {
      updateContainer()
    }
  }

  override fun removeAll(element: ContainerElement) {
    for (i in 0 until size) {
      if (content[i] != null && content[i]!!.element === element) {
        content[i] = null
      }
    }
    updateContainer()
  }

  override fun remove(element: ContainerElement, amount: UInt): UInt {
    Main.logger().debug("Container", "Removing $amount of $element")
    var counter = amount
    var i = 0
    val length = content.size
    try {
      while (i < length) {
        val item = content[i]
        if (item != null && element === item.element) {
          val newAmount = (item.stock - counter).toInt()
          if (newAmount < 0) {
            counter -= item.stock
            content[i] = null
          } else {
            if (newAmount != 0) {
              content[i] = item.use(counter)
            } else {
              content[i] = null
            }
            return 0u
          }
        }
        i++
      }
      return counter
    } finally {
      updateContainer()
    }
  }

  override fun remove(item: Item) {
    Preconditions.checkNotNull(item, "cannot remove a null element")
    if (validOnly && !item.isValid()) {
      return
    }
    var i = 0
    val length = content.size
    while (i < length) {
      if (item == content[i]) {
        content[i] = null
      }
      i++
    }
    updateContainer()
  }

  override fun remove(index: Int) {
    content[index] = null
    updateContainer()
  }

  override fun clear() {
    for (i in content.indices) {
      content[i] = null
    }
    updateContainer()
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

  // TODO implement
  override fun containsAny(element: ContainerElement?): Boolean {
    return false
  }

  override fun get(index: Int): Item? {
    Preconditions.checkPositionIndex(index, size - 1)
    return content[index]
  }

  @Deprecated("")
  override fun getValid(index: Int): Array<Item?> {
    Preconditions.checkPositionIndex(index, size - 1)
    return arrayOf(get(index))
  }

  override fun put(index: Int, item: Item?) {
    Preconditions.checkPositionIndex(index, size - 1)
    require(!(validOnly && item != null && !item.isValid())) { "This container does not allow invalid stacks" }
    content[index] = item
    updateContainer()
  }

  override fun swap(index1: Int, index2: Int) {
    Preconditions.checkPositionIndex(index1, size - 1)
    val item1 = content[index1]
    content[index1] = content[index2]
    content[index2] = item1
    updateContainer()
  }

  protected open fun updateContainer() {
    EventManager.dispatchEvent(ContainerEvent.Changed(this))
  }

  override fun iterator(): MutableIterator<IndexedItem> {
    return object : MutableIterator<IndexedItem> {
      var index: Int = 0

      override fun hasNext(): Boolean {
        return index < size
      }

      override fun next(): IndexedItem {
        return IndexedItem(index, content[index++])
      }

      override fun remove() {
        updateContainer()
        content[index] = null
      }
    }
  }
}
