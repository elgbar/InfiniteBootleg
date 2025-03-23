package no.elg.infiniteBootleg.core.inventory.container.impl

import no.elg.infiniteBootleg.core.events.ItemChangeType
import no.elg.infiniteBootleg.core.inventory.container.SortOrder
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.protobuf.ProtoWorld

/**
 * A container that will auto sort the storage when updated
 *
 * @author kheba
 */
class AutoSortedContainer(
  name: String,
  size: Int = DEFAULT_SIZE,
  private val sortOrder: SortOrder = defaultSortOrder
) : ContainerImpl(name, size) {

  override val type: ProtoWorld.Container.Type get() = ProtoWorld.Container.Type.AUTO_SORTED

  override fun updateContainer(changeType: ItemChangeType?) {
    sortOrder.sort(content)
    super.updateContainer(changeType)
  }

  override fun put(index: Int, item: Item?) {
    require(index in 0 until size) { "Index out of bounds: $index" }
    require(!validOnly || item == null || item.isValid()) { "This container does not allow invalid stacks" }
    content[index] = null
    add(item ?: return)
  }

  companion object {
    val defaultSortOrder = SortOrder.Companion.compileComparator(false, SortOrder.Companion.ELEM_NAME_DESC, SortOrder.Companion.AMOUNT_DESC)
  }
}
