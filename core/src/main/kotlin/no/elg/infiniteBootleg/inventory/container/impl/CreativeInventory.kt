package no.elg.infiniteBootleg.inventory.container.impl

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.Inventory
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.main.Main.Companion.logger

/**
 * A creative storage where most of the methods from [InventoryImpl] is not supported. Only
 * [.holding] is implemented.
 */
class CreativeInventory(override val holder: Entity) : Inventory {
  private val currHolding: Item? = null
  override val selected: Int = 0

  init {
    select(0)
  }

  override fun next() {
    select(selected + 1)
  }

  override fun prev() {
    select(selected - 1)
  }

  override fun holding(): Item? {
    return currHolding
  }

  override val size: Int
    get() = 0

  override val isOpen: Boolean
    get() = false

  override fun open() {
    logger().warn("Cannot open a creative inventory")
  }

  override fun close() {}

  override val container: Container?
    get() = null

  override fun select(index: Int) {
    //    if (index < 0) {
    //      index = ContainerElement.TILE_TYPES + (index % ContainerElement.TILE_TYPES);
    //    }
    //    curr = index % ContainerElement.TILE_TYPES;
    //    final ContainerElement tt = ContainerElement.getTileTypeById(curr + 1);
    //    currHolding = new Item(tt, 1);
  }
}
