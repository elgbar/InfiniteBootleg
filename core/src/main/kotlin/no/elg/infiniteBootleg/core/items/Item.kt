package no.elg.infiniteBootleg.core.items

import no.elg.infiniteBootleg.core.util.toTitleCase
import no.elg.infiniteBootleg.core.world.ContainerElement
import no.elg.infiniteBootleg.core.world.ContainerElement.Companion.asProto
import no.elg.infiniteBootleg.core.world.ContainerElement.Companion.fromProto
import no.elg.infiniteBootleg.core.world.ecs.api.ProtoConverter
import no.elg.infiniteBootleg.protobuf.ContainerKt.item
import no.elg.infiniteBootleg.protobuf.ProtoWorld
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
   * @return A copy with the [Item.maxStock] and [Item.stock] set to [stock]
   */
  fun copyToFit(stock: UInt): Item = element.toItem(maxStock = stock, stock = stock)

  /**
   * Change the charge of this item by [delta] amount
   *
   * @param delta The amount to change the stock by
   * @return The resulting items, empty if depleted, or number of items if the delta is larger than the max stock
   */
  fun change(delta: Int): List<Item> =
    when {
      delta == 0 -> listOf(this)
      delta < 0 -> use(delta.absoluteValue.toUInt())?.let { listOf(it) } ?: emptyList()
      else -> merge(copyToFit(delta.toUInt()))
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
  fun merge(other: Item): List<Item> = mergeAll(listOf(this, other), maxStock)

  /**
   * @return If this item is equal to [other] including the stock left
   */
  fun equalsIncludingStock(other: Any?): Boolean = equals(other) && stock == (other as Item).stock

  /**
   * @return If this item is equal to [other] excluding the stock left
   */
  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object : ProtoConverter<Item, ProtoWorld.Container.Item> {
    const val DEFAULT_MAX_STOCK = 65_536u

    val Item?.stockText: String get() = this?.run { "$stock / $maxStock" } ?: "??? / ???"
    val Item?.displayName: String get() = this?.run { element.displayName.lowercase().toTitleCase().replace('_', ' ') } ?: "<Empty>"

    fun mergeAll(items: List<Item>, newElementMaxStock: UInt = DEFAULT_MAX_STOCK): List<Item> {
      if (items.isEmpty() || items.none { it != items.first() }) {
        return items
      }
      val element = items.first().element
      val totalStock = items.sumOf { it.stock }
      if (totalStock <= newElementMaxStock) {
        return listOf(element.toItem(newElementMaxStock, totalStock))
      } else {
        val mergedItems = (0u until totalStock / newElementMaxStock).mapTo(mutableListOf()) { element.toItem(newElementMaxStock, newElementMaxStock) }
        if (totalStock % newElementMaxStock != 0u) {
          mergedItems += element.toItem(newElementMaxStock, totalStock % newElementMaxStock)
        }
        return mergedItems
      }
    }

    override fun ProtoWorld.Container.Item.fromProto(): Item = element.fromProto().toItem(maxStock.toUInt(), stock.toUInt())

    override fun Item.asProto(): ProtoWorld.Container.Item =
      item {
        stock = this@asProto.stock.toInt()
        maxStock = this@asProto.maxStock.toInt()
        element = this@asProto.element.asProto()
      }
  }
}
