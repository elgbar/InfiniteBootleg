package no.elg.infiniteBootleg.inventory.container.impl

import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.events.ContainerEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.IndexedItem
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.items.Item.Companion.mergeAll
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

  override fun firstEmpty(): Int {
    for (i in 0 until size) {
      if (content[i] == null) {
        return i
      }
    }
    return -1
  }

  override fun first(item: Item?): Int {
    if (item == null) {
      return firstEmpty()
    }
    // if invalid stacks is not allows this cannot have a invalid validate
    if (validOnly && !item.isValid()) {
      return -1
    }
    for (i in 0 until size) {
      val loopTs = content[i]
      if (loopTs != null && loopTs.element === item.element && item.stock <= loopTs.stock) {
        return i
      }
    }
    return -1
  }

  override fun add(tileType: ContainerElement, amount: UInt): UInt {
    var amountNotAdded = amount
    var index = first(tileType)
    if (index < 0) {
      index = firstEmpty()
    }
    if (index < 0) {
      return amount
    } else if (!validOnly) {
      val ts = content[index]
      val item = tileType.toItem(Item.DEFAULT_MAX_STOCK, amount)
      if (ts == null) {
        content[index] = item
      } else {
        val merge = ts.merge(item)
        content[index] = merge[0]
        add(merge[1])
      }
      updateContainer()
      return 0u
    } else {
      val item = tileType.toItem(Item.DEFAULT_MAX_STOCK, amount)
      for (stack in content) {
        if (stack != null && stack.element === tileType && stack.stock < Item.DEFAULT_MAX_STOCK) {
          val needed = Item.DEFAULT_MAX_STOCK - stack.stock
          val given = amountNotAdded.coerceAtMost(needed)
          amountNotAdded -= given
          if (amountNotAdded == 0u) {
            updateContainer()
            return 0u
          }
        }
      }
    }

    val validStacks =
      mergeAll(
        listOf(tileType.toItem(Item.DEFAULT_MAX_STOCK, amountNotAdded)),
        Item.DEFAULT_MAX_STOCK
      )

    var toSkip = -1
    var i = 0
    val length = validStacks.size
    while (i < length) {
      index = firstEmpty()
      if (index < 0) {
        toSkip = i
        break // no more empty slots
      }
      content[index] = validStacks[i]
      i++
    }
    updateContainer()
    // if we do not need to skip anything the loop finished successfully
    if (toSkip == -1) {
      return 0u
    }

    // skip the ones already added, then sum up the rest
    return validStacks.drop(toSkip).sumOf { it.stock }
  }

  override fun removeAll(tileType: ContainerElement) {
    for (i in 0 until size) {
      if (content[i] != null && content[i]!!.element === tileType) {
        content[i] = null
      }
    }
    updateContainer()
  }

  override fun remove(tileType: ContainerElement, amount: UInt): UInt {
    var counter = amount
    var i = 0
    val length = content.size
    while (i < length) {
      val item = content[i]
      if (item != null && tileType === item.element) {
        val newAmount = (item.stock - counter).toInt()
        if (newAmount < 0) {
          counter -= item.stock
          content[i] = null
        } else {
          if (newAmount != 0) {
            item.use(counter)
          } else {
            content[i] = null
          }
          updateContainer()
          return 0u
        }
      }
      i++
    }
    updateContainer()
    return counter
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
  override fun containsAny(tileType: ContainerElement?): Boolean {
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
