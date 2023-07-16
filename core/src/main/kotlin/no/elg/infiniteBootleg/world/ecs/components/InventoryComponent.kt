package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.collections.GdxSet
import ktx.collections.minusAssign
import ktx.collections.plusAssign
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.InventoryKt.item
import no.elg.infiniteBootleg.protobuf.EntityKt.inventory
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.api.EntityParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.asProto

class InventoryComponent(private val maxSize: Int) : EntitySavableComponent {

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

  companion object : EntityParentLoadableMapper<InventoryComponent>() {
    var Entity.inventory by propertyFor(InventoryComponent.mapper)
    var Entity.inventoryOrNull by optionalPropertyFor(InventoryComponent.mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) {
      with(InventoryComponent(protoEntity.inventory.maxSize)) {
        protoEntity.inventory.itemsList.forEach {
          this += Item(Material.fromOrdinal(it.material.ordinal), it.stock.toUInt(), it.maxStock.toUInt())
        }
      }
    }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasInventory()
  }

  override fun EntityKt.Dsl.save() {
    inventory = inventory {
      maxSize = this@InventoryComponent.maxSize
      items += this@InventoryComponent.items.map {
        item {
          material = it.material.asProto()
          stock = it.stock.toInt()
          maxStock = it.maxStock.toInt()
        }
      }
    }
  }
}
