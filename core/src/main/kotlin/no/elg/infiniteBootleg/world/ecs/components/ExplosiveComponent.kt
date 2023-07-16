package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.explosive
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.ecs.api.EntityParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.with

class ExplosiveComponent(var fuse: Float = FUSE_DURATION_SECONDS, val strength: Float = EXPLOSION_STRENGTH.toFloat()) : EntitySavableComponent {

  override fun EntityKt.Dsl.save() {
    explosive = explosive {
      fuse = fuse
      strength = strength
    }
  }

  companion object : EntityParentLoadableMapper<ExplosiveComponent>() {
    val Entity.explosiveComponent by propertyFor(ExplosiveComponent.mapper)
    var Entity.explosiveComponentOrNull by optionalPropertyFor(ExplosiveComponent.mapper)

    /**
     * Randomness to what blocks are destroyed.
     *
     * Lower means more blocks destroyed, but more random holes around the crater.
     *
     * Higher means fewer blocks destroyed but less unconnected destroyed blocks. Note that too
     * large will not look good
     *
     * Minimum value should be above 3 as otherwise the edge of the explosion will clearly be
     * visible
     */
    const val RESISTANCE = 8
    const val FUSE_DURATION_SECONDS = 3f

    /** Maximum explosion radius  */
    const val EXPLOSION_STRENGTH = 40

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) {
      with(ExplosiveComponent(protoEntity.explosive.fuse, protoEntity.explosive.strength))
    }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasExplosive()
  }
}
