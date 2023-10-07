package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.killable
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent

data class KillableComponent(val maxHealth: Int, val health: Int) : EntitySavableComponent {

  val killed: Boolean = health <= 0
  val alive: Boolean = health > 0

  override fun EntityKt.Dsl.save() {
    killable = killable {
      health = this@KillableComponent.health
      maxHealth = this@KillableComponent.maxHealth
    }
  }

  companion object : EntityLoadableMapper<KillableComponent>() {

    const val DEFAULT_MAX_HEALTH = 100

    val Entity.killableComponent by propertyFor(mapper)
    var Entity.killableComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = with(KillableComponent(protoEntity.killable.health, protoEntity.killable.maxHealth))

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasKillable()
  }
}
