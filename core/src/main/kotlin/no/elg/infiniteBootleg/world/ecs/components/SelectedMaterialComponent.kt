package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.screen.hud.CurrentBlock
import no.elg.infiniteBootleg.world.Material

class SelectedMaterialComponent : Component {

  var material: Material = Material.BRICK
    set(value) {
      CurrentBlock.lastMaterial = value
      field = value
    }

  companion object : Mapper<SelectedMaterialComponent>() {
    var Entity.selectedMaterial by propertyFor(SelectedMaterialComponent.mapper)
    var Entity.selectedMaterialOrNull by optionalPropertyFor(SelectedMaterialComponent.mapper)
  }
}
