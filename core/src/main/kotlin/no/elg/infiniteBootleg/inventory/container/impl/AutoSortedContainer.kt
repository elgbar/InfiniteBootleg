package no.elg.infiniteBootleg.inventory.container.impl

import no.elg.infiniteBootleg.inventory.container.SortOrder
import no.elg.infiniteBootleg.items.Item
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

  override fun updateContainer() {
    super.updateContainer()
    sortOrder.sort(content)
  }

  override fun put(index: Int, item: Item?) {
    require(index in 0 until size) { "Index out of bounds: $index" }
    require(!validOnly || item == null || item.isValid()) { "This container does not allow invalid stacks" }
    content[index] = null
    add(item ?: return)
  }

  companion object {
    val defaultSortOrder = SortOrder.compileComparator(false, SortOrder.ELEM_NAME_DESC, SortOrder.AMOUNT_DESC)
  }
}
