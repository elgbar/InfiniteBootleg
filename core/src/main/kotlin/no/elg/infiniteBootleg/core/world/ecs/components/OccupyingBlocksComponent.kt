package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.occupyingBlocks
import no.elg.infiniteBootleg.protobuf.ProtoWorld

/**
 * @param hardLink If destroying a [EntityMarkerBlock] will also destroy the entity (and all other linked blocks & entities)
 */
class OccupyingBlocksComponent(val hardLink: Boolean) : EntitySavableComponent {
  /**
   * List of locations that this entity is occupying
   */
  val occupying: GdxArray<EntityMarkerBlock> = GdxArray(false, 1)

  override fun hudDebug(): String = "Hard link? $hardLink occupying ${occupying.map { it.hudDebug() }}"

  override fun EntityKt.Dsl.save() {
    occupyingBlocks = occupyingBlocks {
      hardLink = this@OccupyingBlocksComponent.hardLink
    }
  }

  companion object : EntityLoadableMapper<OccupyingBlocksComponent>() {
    val Entity.occupyingLocations get() = occupyingBlocksComponent.occupying
    val Entity.occupyingLocationsOrNull get() = occupyingBlocksComponentOrNull?.occupying
    val Entity.occupyingBlocksComponent by propertyFor(mapper)
    var Entity.occupyingBlocksComponentOrNull by optionalPropertyFor(mapper)

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasOccupyingBlocks()
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): OccupyingBlocksComponent? = safeWith { OccupyingBlocksComponent(protoEntity.occupyingBlocks.hardLink) }
  }
}
