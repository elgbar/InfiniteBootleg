package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.selectedItem
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent.Companion.inventoryComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.asProto
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.fromProto

class SelectedInventoryItemComponent(var material: Material = Material.STONE) : EntitySavableComponent {

  companion object : EntityLoadableMapper<SelectedInventoryItemComponent>() {
    var Entity.selectedInventoryItemComponent by propertyFor(SelectedInventoryItemComponent.mapper)
    var Entity.selectedInventoryItemComponentOrNull by optionalPropertyFor(SelectedInventoryItemComponent.mapper)
    val Entity.selectedItem: Item?
      get() {
        val material = selectedInventoryItemComponentOrNull?.material ?: return null
        val inventory = inventoryComponentOrNull ?: return null
        return inventory[material]
      }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasSelectedItem()

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): SelectedInventoryItemComponent =
      with(SelectedInventoryItemComponent(protoEntity.selectedItem.material.fromProto()))
  }

  override fun EntityKt.Dsl.save() {
    selectedItem = selectedItem {
      material = this@SelectedInventoryItemComponent.material.asProto()
    }
  }
}
