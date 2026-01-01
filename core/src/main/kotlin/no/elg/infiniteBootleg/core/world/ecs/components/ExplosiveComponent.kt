package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.explosive
import no.elg.infiniteBootleg.protobuf.ProtoWorld

data class ExplosiveComponent(var fuse: Float = FUSE_DURATION_SECONDS, val strength: Float = EXPLOSION_STRENGTH.toFloat()) : EntitySavableComponent {

  override fun EntityKt.Dsl.save() {
    explosive = explosive {
      fuse = fuse
      strength = strength
    }
  }

  override fun hudDebug(): String = "fuse: $fuse, strength: $strength"

  companion object : EntityLoadableMapper<ExplosiveComponent>() {
    val Entity.explosiveComponent by propertyFor(mapper)
    var Entity.explosiveComponentOrNull by optionalPropertyFor(mapper)

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

    /** Default explosion radius  */
    const val EXPLOSION_STRENGTH = 20

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = safeWith { ExplosiveComponent(protoEntity.explosive.fuse, protoEntity.explosive.strength) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasExplosive()
  }
}
