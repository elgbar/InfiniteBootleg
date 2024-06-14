package no.elg.infiniteBootleg.inventory.container.impl

import com.google.common.base.Preconditions
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.events.ContainerEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.IndexedItem
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ContainerElement

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
      val item = content[i]
      if (item != null && item.element === element) {
        content[i] = null
      }
    }
    updateContainer()
  }

  override fun remove(element: ContainerElement, amount: UInt): UInt {
    if (amount == 0u) return 0u
    logger.debug { "Removing $amount of $element" }
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
    if (validOnly && !item.isValid() || item.stock == 0u) {
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
    if (content[index] != null) {
      content[index] = null
      updateContainer()
    }
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
