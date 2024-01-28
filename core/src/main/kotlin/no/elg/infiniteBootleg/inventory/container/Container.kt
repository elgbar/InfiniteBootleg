package no.elg.infiniteBootleg.inventory.container

import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.items.Item.Companion.DEFAULT_MAX_STOCK
import no.elg.infiniteBootleg.world.ContainerElement

/**
 * An interface for things that holds items.
 *
 *
 * If the container can only hold valid stacks (checked with [Item.isValid]) is up to the
 * implementation.
 *
 *
 * However for [.getValid] the returned Item <bold>MUST</bold> be a valid stacks (or
 * null) if this is not the desired use [.get] for invalid stacks
 *
 *
 * TODO implement a chest (that can be opened, somehow) TODO let some entities have inventories
 *
 * @author kheba
 */
interface Container : Iterable<ContainerSlot> {
  /**
   * @return The name of the container
   */
  /**
   * @param name The new name of the container
   */
  var name: String

  /**
   * @return How many slots this container holds
   */
  val size: Int

  val validOnly: Boolean

  /**
   * @return The first empty slot in the container, return a negative number if none is found
   */
  fun firstEmpty(): Int

  /**
   * @param tileType The tileType to match against
   * @return The index of the first tile where there are at least [Item.getStock] tiles. If
   * the input is null, this method is identical to [.firstEmpty]
   */
  fun first(containerElement: ContainerElement?): Int {
    if (containerElement == null) {
      return firstEmpty()
    }
    return first(containerElement.toItem(DEFAULT_MAX_STOCK, 0u))
  }

  /**
   * x
   *
   * @param Item The Item to match against
   * @return The index of the first tile where there are at least `Item.getStock` tiles,
   * return negative number if not found. If the input is null, this method is identical to
   * [.firstEmpty]
   */
  fun first(item: Item?): Int

  /**
   * Add an item to the container
   *
   * @return How many of the given tiletype not added
   * @throws IllegalArgumentException if `ContainerElement` is `null` or amount is less
   * than zero
   */
  fun add(tileType: ContainerElement, amount: UInt): UInt

  /**
   * Add one or more items to the container
   *
   * @param Item What to add
   */
  fun add(vararg items: Item): List<Item?>? {
    return add(items.toList())
  }

  /**
   * Add one or more items to the container
   *
   * @param items What to add
   * @return A list of all tiles not added, the returned stack might not be valid.
   * @throws IllegalArgumentException if one of the `Item`s is `null`
   */
  fun add(items: List<Item>): List<Item?>? {
    val collector: MutableMap<ContainerElement, UInt> = HashMap()

    // tally up how many we got of each type
    for (stack in items) {
      //            if (stack == null) { continue; }
      collector[stack.element] = collector.getOrDefault(stack.element, 0u) + stack.stock
    }

    val notAdded: MutableList<Item?> = ArrayList()

    // then add them all type by type
    for ((element, stock) in collector) {
      val failedToAdd = add(element, stock)
      // if any tiles failed to be added, add them here
      if (failedToAdd > 0u) {
        notAdded.add(element.toItem(Item.DEFAULT_MAX_STOCK, failedToAdd))
      }
    }
    return notAdded
  }

  /** Remove all tile stacks with the given tile type  */
  fun removeAll(tileType: ContainerElement)

  /**
   * Remove `amount` of the given tile type
   *
   * @param amount How many to remove
   * @param tileType What tile to remove
   * @return How many tiles that were not removed
   */
  fun remove(tileType: ContainerElement, amount: UInt): UInt

  /**
   * Remove tile stacks in the container that match the given element
   *
   * @param Item The tile validate to remove
   * @throws IllegalArgumentException if one of the `Item`s is `null`
   */
  fun remove(item: Item)

  /**
   * Remove tile at index
   *
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to
   * [.getSize]
   */
  fun remove(index: Int)

  /** Clear the container of all tile stacks  */
  fun clear()

  /**
   * @param Item The item to check for
   * @return False if `Item` is null, true if this container has the given `Item`
   */
  fun contains(item: Item?): Boolean

  /**
   * @return If this container has the given `Item`, `false` is returned if tiletype is
   * `null` or if size is less than 0
   */
  fun contains(tileType: ContainerElement?, size: Int): Boolean {
    if (tileType == null || size < 0) {
      return false
    }
    return contains(tileType.toItem(Item.DEFAULT_MAX_STOCK, size.toUInt()))
  }

  /**
   * @return If the container has an item of this ContainerElement
   */
  fun containsAny(tileType: ContainerElement?): Boolean

  /**
   * This method returns the [Item] as is at the given location, there will be no check
   *
   * @return The `Item` at the given location, `null` if there is nothing there
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to
   * [.getSize]
   */
  operator fun get(index: Int): Item?

  /**
   * @param index The index of the item to get
   * @return An array of valid stacks (will pass [Item.isValid]) from the tile stack at the
   * given location
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to
   * [.getSize]
   */
  @Deprecated("")
  fun getValid(index: Int): Array<Item?>

  /**
   * Overwrite a the given `Item` at `index`. If the given tile validate is `null`
   * it is the same as calling [.remove].
   *
   * @param index The index to place the `Item` at
   * @param Item The Item to put at `index`
   * @throws IndexOutOfBoundsException if the index is less than 0 or greater than or equal to
   * [.getSize]
   */
  fun put(index: Int, item: Item?)

  /**
   * @return The underlying array of the container
   */
  val content: Array<Item?>

  /** Update the GUI of the displayed container.  */
  fun updateContainer()
}
