package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.decaying
import no.elg.infiniteBootleg.protobuf.ProtoWorld

data class DecayingComponent(var timeLeftSeconds: Double) : EntitySavableComponent {

  override fun hudDebug(): String = "$timeLeftSeconds s"

  override fun EntityKt.Dsl.save() {
    decaying = decaying {
      timeLeftSeconds = this@DecayingComponent.timeLeftSeconds
    }
  }

  companion object : EntityLoadableMapper<DecayingComponent>() {

    val Entity.decayComponent by propertyFor(mapper)
    var Entity.decayComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = safeWith { DecayingComponent(protoEntity.decaying.timeLeftSeconds) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasDecaying()
  }
}
