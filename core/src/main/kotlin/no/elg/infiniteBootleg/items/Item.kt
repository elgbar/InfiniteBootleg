package no.elg.infiniteBootleg.items

import no.elg.infiniteBootleg.protobuf.EntityKt.InventoryKt.item
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ContainerElement
import no.elg.infiniteBootleg.world.ContainerElement.Companion.asProto
import no.elg.infiniteBootleg.world.ContainerElement.Companion.fromProto
import no.elg.infiniteBootleg.world.ecs.api.ProtoConverter
import kotlin.math.absoluteValue

sealed interface Item {

  val element: ContainerElement

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

  /**
   * Change the charge of this item by [delta] amount
   *
   * @param delta The amount to change the stock by
   * @return The resulting items, empty if depleted, or number of items if the delta is larger than the max stock
   */
  fun change(delta: Int): List<Item> {
    return if (delta == 0) {
      listOf(this)
    } else if (delta < 0) {
      use(delta.absoluteValue.toUInt())?.let { listOf(it) } ?: emptyList()
    } else {
      Item.mergeAll(listOf(this), maxStock)
    }
  }

  fun willBeDepleted(usages: UInt = 1u): Boolean = usages >= stock

  /**
   * @return If this item can be used [usages] amount of times
   */
  fun canBeUsed(usages: UInt = 1u): Boolean = usages <= stock

  fun isValid(): Boolean = stock <= maxStock

  /**
   * Merge two items, they must be of the same type, material/tool, and max stock to be merged otherwise `null` is returned
   *
   * @return The resulting item, or the same items if the items cannot be merged
   */
  fun merge(other: Item): List<Item> {
    return mergeAll(listOf(this, other))
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

    fun mergeAll(items: List<Item>, newElementMaxStock: UInt = DEFAULT_MAX_STOCK): List<Item> {
      if (items.isEmpty() || items.none { it != items.first() }) {
        return items
      }
      var remainingStock = items.sumOf { it.stock }
      val sortedMaxStock = items.sortedByDescending { it.maxStock }
      val result = mutableListOf<Item>()
      for (item in sortedMaxStock) {
        val stockToPlace = remainingStock.coerceAtMost(item.maxStock)
        remainingStock -= stockToPlace
        result += item.element.toItem(item.maxStock, stockToPlace)
        if (stockToPlace == 0u) {
          break
        }
      }
      while (remainingStock > 0u) {
        val stockToPlace = remainingStock.coerceAtMost(newElementMaxStock)
        remainingStock -= stockToPlace
        result += items.first().element.toItem(newElementMaxStock, stockToPlace)
      }
      return result
    }

    override fun ProtoWorld.Entity.Inventory.Item.fromProto(): Item = element.fromProto().toItem(maxStock.toUInt(), stock.toUInt())

    override fun Item.asProto(): ProtoWorld.Entity.Inventory.Item =
      item {
        stock = this@asProto.stock.toInt()
        maxStock = this@asProto.maxStock.toInt()
        element = this@asProto.element.asProto()
      }
  }
}
