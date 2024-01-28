package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.collections.GdxSet
import ktx.collections.minusAssign
import ktx.collections.plusAssign
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.items.Item.Companion.asProto
import no.elg.infiniteBootleg.items.Item.Companion.fromProto
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.inventory
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.ContainerElement
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.restriction.AuthoritativeOnlyComponent

class InventoryComponent(private val maxSize: Int) : EntitySavableComponent, AuthoritativeOnlyComponent {

  private val items = GdxSet<Item>()

  private fun replace(item: Item) {
    items -= item
    items += item
  }

  operator fun plusAssign(item: Item) {
    val existing = this[item]
    val updatedItem = existing?.merge(item)?.firstOrNull() ?: item // TODO THIS is wrong
    replace(updatedItem)
  }

  operator fun minusAssign(item: Item) {
    val existing = this[item]
    val updatedItem = existing?.use(item.stock) ?: return
    replace(updatedItem)
  }

  operator fun get(item: Item): Item? = items.find { it == item }
  operator fun get(element: ContainerElement): Item? = items.find { it.element == element }

  fun getAll(element: ContainerElement): List<Item> = items.filter { it.element == element }

  operator fun contains(item: Item): Boolean = items.any { it == item }
  operator fun contains(element: ContainerElement): Boolean = items.any { it.element == element }

  /**
   * @return If the item was used
   */
  fun use(element: ContainerElement, usages: UInt = 1u): Boolean {
    val item = getAll(element).firstOrNull { it.canBeUsed(usages) } ?: return false
    val updatedItem = item.use(usages) ?: return false
    replace(updatedItem)
    return true
  }

  companion object : EntityLoadableMapper<InventoryComponent>() {
    var Entity.inventoryComponent by propertyFor(InventoryComponent.mapper)
    var Entity.inventoryComponentOrNull by optionalPropertyFor(InventoryComponent.mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) =
      safeWith {
        InventoryComponent(protoEntity.inventory.maxSize).apply {
          protoEntity.inventory.itemsList.forEach { this += it.fromProto() }
        }
      }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasInventory()
  }

  override fun EntityKt.Dsl.save() {
    inventory = inventory {
      maxSize = this@InventoryComponent.maxSize
      items += this@InventoryComponent.items.map { it.asProto() }
    }
  }
}
