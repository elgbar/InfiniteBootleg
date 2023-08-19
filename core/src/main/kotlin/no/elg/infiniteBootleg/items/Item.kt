package no.elg.infiniteBootleg.items

import no.elg.infiniteBootleg.items.InventoryElement.Companion.asProto
import no.elg.infiniteBootleg.items.InventoryElement.Companion.fromProto
import no.elg.infiniteBootleg.protobuf.EntityKt.InventoryKt.item
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.ProtoConverter

sealed interface Item {

  val element: InventoryElement

  /**
   * Maximum amount of this item that can be stacked
   */
  val maxStock: UInt

  /**
   * Current amount of this item
   */
  val stock: UInt

  /**
   * The type of this item
   */
  val itemType: ItemType

  /**
   * Change the charge of this item by [usages] amount
   *
   * @return The resulting item, or `null` if the item would be depleted
   */
  fun use(usages: UInt = 1u): Item?

  fun willBeDepleted(usages: UInt = 1u): Boolean = usages >= stock

  /**
   * @return If this item can be used [usages] amount of times
   */
  fun canBeUsed(usages: UInt = 1u): Boolean = usages <= stock

  /**
   * Merge two items, they must be of the same type, material/tool, and max stock to be merged otherwise `null` is returned
   *
   * @return The resulting item, or `null` if the items cannot be merged
   */
  fun merge(other: Item): Item? {
    require(other == this) { "Cannot merge items of different types, material/tool, or max stock" }
    return when (this) {
      is MaterialItem -> MaterialItem(element, maxStock, stock + other.stock)
      is ToolItem -> ToolItem(element, maxStock, stock + other.stock)
    }
  }

  /**
   * @return If this item is equal to [other] including the stock left
   */
  fun equalsIncludingStock(other: Any?): Boolean = equals(other) && stock == (other as Item).stock

  /**
   * @return If this item is equal to [other] excluding the stock left
   */
  override fun equals(other: Any?): Boolean

  companion object : ProtoConverter<Item, ProtoWorld.Entity.Inventory.Item> {
    const val DEFAULT_MAX_STOCK = 65_536u

    override fun ProtoWorld.Entity.Inventory.Item.fromProto(): Item =
      element.fromProto().toItem(maxStock.toUInt(), stock.toUInt())

    override fun Item.asProto(): ProtoWorld.Entity.Inventory.Item = item {
      stock = this@asProto.stock.toInt()
      maxStock = this@asProto.maxStock.toInt()
      element = this@asProto.element.asProto()
    }
  }
}
