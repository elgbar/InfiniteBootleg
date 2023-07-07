package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock

class OccupyingBlocksComponent : Component {
  /**
   * List of locations that this entity is occupying
   */
  val occupying: GdxArray<EntityMarkerBlock> = GdxArray(false, 8)

  companion object : Mapper<OccupyingBlocksComponent>() {
    val Entity.occupyingLocations get() = occupyingComponent.occupying
    val Entity.occupyingLocationsOrNull get() = occupyingComponentOrNull?.occupying
    val Entity.occupyingComponent by propertyFor(OccupyingBlocksComponent.mapper)
    var Entity.occupyingComponentOrNull by optionalPropertyFor(OccupyingBlocksComponent.mapper)
  }
}
