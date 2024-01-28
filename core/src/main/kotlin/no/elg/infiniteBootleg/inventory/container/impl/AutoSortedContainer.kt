package no.elg.infiniteBootleg.inventory.container.impl

import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.inventory.container.SortOrder
import no.elg.infiniteBootleg.items.Item

/**
 * A container that will auto sort the storage when updated
 *
 * @author kheba
 */
class AutoSortedContainer @JvmOverloads constructor(
  size: Int,
  name: String,
  disallowInvalid: Boolean = true,
  sortOrder: SortOrder = SortOrder(
    false,
    SortOrder.TT_NAME_DESC,
    SortOrder.AMOUNT_DESC
  )
) : ContainerImpl(size, name, disallowInvalid) {
  // do not allow change of valid stacks as the implementation would be hard to do correctly
  private val disallowInvalid: Boolean

  // You can modify the sortOrder directly
  val sortOrder: SortOrder

  /**
   * A normal container that disallows invalid [Item]s and sorts first by name then by tile
   * amount in an descending order, with the name 'Container'
   *
   * @param size The size of the container
   */
  constructor(size: Int) : this(size, "Auto Sorted Container")

  /**
   * @param size The size of the container
   * @param disallowInvalid if this container does not allow invalid [Item]s
   * @param sortOrder The way to sort the validate each time it is modified
   */
  /**
   * A container that sorts first by name then by tile amount in an descending order
   *
   * @param size The size of the container
   * @param disallowInvalid if this container does not allow invalid [Item]s
   */
  /**
   * A normal container that disallows invalid [Item]s and sorts first by name then by tile
   * amount in an descending order
   *
   * @param size The size of the container
   */
  init {
    Preconditions.checkNotNull(sortOrder, "The sort order cannot be null")

    this.disallowInvalid = disallowInvalid
    this.sortOrder = sortOrder
  }

  override fun updateContainer() {
    super.updateContainer()
    sortOrder.sort(content)
  }

  override fun put(index: Int, item: Item?) {
    Preconditions.checkPositionIndex(index, size - 1)
    require(!(validOnly && item != null && !item.isValid())) { "This container does not allow invalid stacks" }
    content[index] = null
    add(item ?: return)
  }
}
