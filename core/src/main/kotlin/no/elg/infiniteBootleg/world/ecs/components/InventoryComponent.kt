package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.collections.GdxSet
import ktx.collections.minusAssign
import ktx.collections.plusAssign
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.world.Material

class InventoryComponent(private val maxSize: Int) : Component {

  private val items = GdxSet<Item>()

  private fun replace(item: Item) {
    items -= item
    items += item
  }

  operator fun plusAssign(item: Item) {
    val existing = this[item]?.stock
    val updatedItem = if (existing != null) {
      Item(item.material, existing + item.stock)
    } else {
      if (items.size >= maxSize) {
        return
      }
      item
    }
    replace(updatedItem)
  }

  operator fun minusAssign(item: Item) {
    val existing = this[item]?.stock
    val updatedItem = if (existing != null) {
      Item(item.material, existing - item.stock)
    } else {
      item
    }
    replace(updatedItem)
  }

  operator fun get(item: Item): Item? = items.find { it == item }
  operator fun get(material: Material): Item? = items.find { it.material == material }

  fun getAll(material: Material): List<Item> = items.filter { it.material == material }
  operator fun contains(material: Material): Boolean = items.any { it.material == material }
  operator fun contains(item: Item): Boolean = items.any { it == item }

  /**
   * @return If the item was used
   */
  fun use(material: Material, usages: UInt = 1u): Boolean {
    val item = getAll(material).firstOrNull { it.canBeUsed(usages) } ?: return false
    val updatedItem = item.use(usages) ?: return false
    replace(updatedItem)
    return true
  }

  companion object : Mapper<InventoryComponent>() {
    var Entity.inventory by propertyFor(InventoryComponent.mapper)
    var Entity.inventoryOrNull by optionalPropertyFor(InventoryComponent.mapper)
  }
}
