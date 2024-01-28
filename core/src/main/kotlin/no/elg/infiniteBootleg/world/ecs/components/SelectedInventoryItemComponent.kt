package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.selectedItem
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.ContainerElement
import no.elg.infiniteBootleg.world.ContainerElement.Companion.asProto
import no.elg.infiniteBootleg.world.ContainerElement.Companion.fromProto
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.restriction.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent.Companion.inventoryComponentOrNull

class SelectedInventoryItemComponent(var element: ContainerElement) : EntitySavableComponent, AuthoritativeOnlyComponent {

  companion object : EntityLoadableMapper<SelectedInventoryItemComponent>() {
    var Entity.selectedInventoryItemComponent by propertyFor(SelectedInventoryItemComponent.mapper)
    var Entity.selectedInventoryItemComponentOrNull by optionalPropertyFor(SelectedInventoryItemComponent.mapper)
    val Entity.selectedItem: Item?
      get() {
        val type = selectedInventoryItemComponentOrNull?.element ?: return null
        val inventory = inventoryComponentOrNull ?: return null
        return inventory[type]
      }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasSelectedItem()

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): SelectedInventoryItemComponent? =
      safeWith { SelectedInventoryItemComponent(protoEntity.selectedItem.element.fromProto()) }
  }

  override fun EntityKt.Dsl.save() {
    selectedItem = selectedItem {
      element = this@SelectedInventoryItemComponent.element.asProto()
    }
  }
}
