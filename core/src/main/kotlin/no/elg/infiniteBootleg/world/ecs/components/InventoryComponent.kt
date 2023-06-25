package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.world.Material

class InventoryComponent(private val maxSize: Int) : Component {

  private val items = mutableSetOf<Item>()

  operator fun plusAssign(item: Item) {
    val existing = items.find { it.material == item.material }?.charge
    items += if (existing != null) {
      Item(item.material, existing + item.charge)
    } else {
      if (items.size >= maxSize) {
        return
      }
      item
    }
  }

  operator fun minusAssign(item: Item) {
    val existing = items.find { it.material == item.material }?.charge
    items -= if (existing != null) {
      Item(item.material, existing - item.charge)
    } else {
      item
    }
  }

  operator fun get(material: Material): Item? = items.find { it.material == material }
  operator fun contains(material: Material): Boolean = items.any { it.material == material }
  operator fun contains(item: Item): Boolean = items.any { it == item }

  companion object : Mapper<InventoryComponent>() {
    var Entity.inventory by propertyFor(InventoryComponent.mapper)
    var Entity.inventoryOrNull by optionalPropertyFor(InventoryComponent.mapper)
  }
}
