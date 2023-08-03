package no.elg.infiniteBootleg.world.ecs.components.additional

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.ecs.api.AdditionalComponentsSavableComponent
import no.elg.infiniteBootleg.world.ecs.api.StatelessAdditionalComponentsLoadableMapper

class OccupyingBlocksComponent : AdditionalComponentsSavableComponent {
  /**
   * List of locations that this entity is occupying
   */
  val occupying: GdxArray<EntityMarkerBlock> = GdxArray(false, 8)

  companion object : StatelessAdditionalComponentsLoadableMapper<OccupyingBlocksComponent>() {
    val Entity.occupyingLocations get() = occupyingBlocksComponent.occupying
    val Entity.occupyingLocationsOrNull get() = occupyingBlocksComponentOrNull?.occupying
    val Entity.occupyingBlocksComponent by propertyFor(OccupyingBlocksComponent.mapper)
    var Entity.occupyingBlocksComponentOrNull by optionalPropertyFor(OccupyingBlocksComponent.mapper)

    override fun ProtoWorld.Entity.AdditionalComponents.checkShouldLoad(): Boolean = hasOccupyingBlocks()
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.AdditionalComponents): OccupyingBlocksComponent = with(OccupyingBlocksComponent())
  }

  override fun EntityKt.AdditionalComponentsKt.Dsl.save() {
    occupyingBlocks = true
  }
}
