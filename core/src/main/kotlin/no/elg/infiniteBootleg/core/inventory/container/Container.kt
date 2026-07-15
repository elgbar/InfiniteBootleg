package no.elg.infiniteBootleg.core.inventory.container

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.inventory.container.Container.Companion.NOT_FOUND
import no.elg.infiniteBootleg.core.inventory.container.impl.AutoSortedContainer
import no.elg.infiniteBootleg.core.inventory.container.impl.ContainerImpl
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.items.Item.Companion.asProto
import no.elg.infiniteBootleg.core.items.Item.Companion.fromProto
import no.elg.infiniteBootleg.core.world.ContainerElement
import no.elg.infiniteBootleg.core.world.ecs.api.ProtoConverter
import no.elg.infiniteBootleg.protobuf.ContainerKt
import no.elg.infiniteBootleg.protobuf.container
import no.elg.infiniteBootleg.protobuf.itemOrNull
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Container as ProtoContainer

/**
 * An interface for things that holds items.
 *
 *
 * If the container can only hold valid stacks (checked with [no.elg.infiniteBootleg.core.items.Item.isValid]) is up to the
 * implementation.
 *
 *
 * @author kheba
 */
interface Container : Iterable<IndexedItem> {
  /**
   * @return The name of the container
   */
  val name: String

  /**
   * @return How many slots this container holds
   */
  val size: Int

  val validOnly: Boolean get() = true

  fun isEmpty(): Boolean = indexOfFirstEmpty() < 0
  fun isNotEmpty(): Boolean = !isEmpty()

  /**
   * @return The first empty slot in the container or [NOT_FOUND] if there is no empty slot
   */
  fun indexOfFirstEmpty(): Int

  /**
   * @param element The element to match against
   * @return The index of the first element of type `element` and where the stock is less than max stock, or [NOT_FOUND] if none is found
   */
  fun indexOfFirstNonFull(element: ContainerElement): Int

  /**
   * @param element The element to match against
   * @return The index of in the container where the [element] can be added, or [NOT_FOUND] if the container does not contain such item
   */
  fun indexOfFirstCanAdd(element: ContainerElement): Int = indexOfFirstNonFull(element).let { if (it == NOT_FOUND) indexOfFirstEmpty() else it }

  /**
   * @param element The element to match against
   * @return The index of the first element of type `element` or [NOT_FOUND] if the container does not contain such item
   */
  fun indexOfFirst(element: ContainerElement): Int

  /**
   * @param filter The filter to match against
   * @return The index of the first slot that matches the given filter, or [NOT_FOUND] if the container does not contain such item
   */
  fun indexOfFirst(filter: (Item?) -> Boolean): Int

  /**
   * Add an item to the container
   *
   * @return How many of the given element not added
   * @throws IllegalArgumentException if `ContainerElement` is `null` or amount is less
   * than zero
   */
  fun add(element: ContainerElement, amount: UInt): UInt

  /**
   * Add one or more items to the container
   *
   * @param items What to add
   */
  fun add(vararg items: Item): List<Item> = add(items.toList())

  /**
   * Add one or more items to the container
   *
   * @param items What to add
   * @return A list of all elements not added, the returned stack might not be valid.
   * @throws IllegalArgumentException if one of the `Item`s is `null`
   */
  fun add(items: List<Item>): List<Item>

  /**
   * Check if there exists at least `amount` of the given element type in the container
   */
  fun exists(element: ContainerElement, amount: UInt): Boolean

  /**
   * Count how many of the given element type is in the container
   */
  fun count(element: ContainerElement): UInt

  /** Remove all element stacks with the given element type  */
  fun removeAll(element: ContainerElement)

  /**
   * Remove `amount` of the given element type
   *
   * @param amount How many to remove
   * @param element What element to remove
   * @return How many elements that were not removed, i.e., `0u` means everything was removed
   */
  fun remove(element: ContainerElement, amount: UInt): UInt

  /**
   * Remove `amount` of the given item's element type
   *
   * @param amount How many to remove
   * @param item What item's element to remove
   * @return How many elements that were not removed, i.e., `0u` means everything was removed
   */
  fun remove(item: Item, amount: UInt): UInt

  /**
   * Remove element stacks in the container that match the given element
   *
   * @param item The item to remove
   */
  fun remove(item: Item)

  /**
   * Remove item at index
   *
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to [size]
   */
  fun remove(index: Int)

  /** Clear the container of everything  */
  fun clear()

  /**
   * @param item The item to check for
   * @return False if `Item` is null, true if this container has the given `Item`
   */
  operator fun contains(item: Item?): Boolean

  /**
   * @return If this container has the given `Item`, `false` is returned if [ContainerElement] is
   * `null` or if size is less than 0
   */
  fun contains(element: ContainerElement?, size: Int): Boolean {
    if (element == null || size < 0) {
      return false
    }
    return contains(element.toItem(stock = size.toUInt()))
  }

  /**
   * This method returns the [Item] as is at the given location, there will be no check
   *
   * @return The `Item` at the given location, `null` if there is nothing there
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to [size]
   */
  operator fun get(index: Int): Item?

  /**
   * Overwrite the given `Item` at `index`. If the given tile validate is `null`
   * it is the same as calling [remove].
   *
   * @param index The index to place the `Item` at
   * @param item The Item to put at `index`
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to [size]
   */
  operator fun set(index: Int, item: Item?)

  fun swap(index1: Int, index2: Int)

  /**
   * @return The underlying array of the container
   */
  val content: Array<Item?>

  operator fun plusAssign(item: Item) {
    add(item)
  }

  val type: ProtoContainer.Type

  companion object : ProtoConverter<Container, ProtoContainer> {
    private val logger = KotlinLogging.logger {}

    const val NOT_FOUND = -1

    override fun ProtoContainer.fromProto(): Container =
      when (type) {
        ProtoContainer.Type.GENERIC -> ContainerImpl(name, maxSize)
        ProtoContainer.Type.AUTO_SORTED -> AutoSortedContainer(name, maxSize)
        else -> ContainerImpl(name, maxSize).also { logger.error { "Unknown container type $type" } }
      }.apply {
        // note: if an index does not exist in the proto, the slot is implicitly empty
        for (indexedItem in itemsList) {
          content[indexedItem.index] = indexedItem.itemOrNull?.fromProto()
        }
      }

    override fun Container.asProto(): ProtoContainer =
      container {
        maxSize = this@asProto.size
        name = this@asProto.name
        type = this@asProto.type
        // Only add items with a value, indices without a value are implicitly null
        items += this@asProto.content.mapIndexed { containerIndex, maybeItem ->
          maybeItem?.let {
            ContainerKt.indexedItem {
              index = containerIndex
              item = it.asProto()
            }
          }
        }.filterNotNull()
      }
  }
}
