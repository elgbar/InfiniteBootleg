package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent.Companion.inventoryOrNull

class SelectedInventoryItemComponent : Component {

  var material: Material = Material.BRICK

  companion object : Mapper<SelectedInventoryItemComponent>() {
    var Entity.selected by propertyFor(SelectedInventoryItemComponent.mapper)
    var Entity.selectedOrNull by optionalPropertyFor(SelectedInventoryItemComponent.mapper)
    val Entity.selectedItem: Item?
      get() {
        val material = selectedOrNull?.material ?: return null
        val inventory = inventoryOrNull ?: return null
        return inventory[material]
      }
  }
}
