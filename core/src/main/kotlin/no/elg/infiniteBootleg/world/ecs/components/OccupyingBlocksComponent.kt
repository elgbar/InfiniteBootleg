package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent

class OccupyingBlocksComponent : EntitySavableComponent {
  /**
   * List of locations that this entity is occupying
   */
  val occupying: GdxArray<EntityMarkerBlock> = GdxArray(false, 8)

  companion object : EntityLoadableMapper<OccupyingBlocksComponent>() {
    val Entity.occupyingLocations get() = occupyingBlocksComponent.occupying
    val Entity.occupyingLocationsOrNull get() = occupyingBlocksComponentOrNull?.occupying
    val Entity.occupyingBlocksComponent by propertyFor(OccupyingBlocksComponent.mapper)
    var Entity.occupyingBlocksComponentOrNull by optionalPropertyFor(OccupyingBlocksComponent.mapper)

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasOccupyingBlocks()
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): OccupyingBlocksComponent = with(OccupyingBlocksComponent())
    val PROTO_OCCUPYING_BLOCKS = ProtoWorld.Entity.OccupyingBlocks.getDefaultInstance()
  }

  override fun EntityKt.Dsl.save() {
    occupyingBlocks = PROTO_OCCUPYING_BLOCKS
  }
}
