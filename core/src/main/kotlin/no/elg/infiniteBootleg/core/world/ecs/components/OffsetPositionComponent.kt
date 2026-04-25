package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.core.util.BlockUnitF
import no.elg.infiniteBootleg.core.util.Compacted2Float
import no.elg.infiniteBootleg.core.util.compactFloat
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.offsetPositionOrNull
import no.elg.infiniteBootleg.protobuf.vector2f

/**
 * Marks how the ashley and box2d are offset from one another.
 *
 * Base is box2d, add [offsetX] and [offsetY] to get the ashley position
 */
data class OffsetPositionComponent(var offsetX: BlockUnitF, var offsetY: BlockUnitF) : EntitySavableComponent {
  override fun EntityKt.Dsl.save() {
    offsetPosition = vector2f {
      x = offsetX
      y = offsetY
    }
  }

  override fun hudDebug(): String = stringifyCompactLoc(offsetX, offsetY)

  companion object : EntityLoadableMapper<OffsetPositionComponent>() {

    val Entity.compactOffset: Compacted2Float get() = offsetPositionComponentOrNull?.run { compactFloat(offsetX, offsetY) } ?: 0L
    val Entity.offsetPositionComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): OffsetPositionComponent? =
      protoEntity.offsetPositionOrNull?.let { safeWith { OffsetPositionComponent(it.x, it.y) } }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasOffsetPosition()
  }
}
