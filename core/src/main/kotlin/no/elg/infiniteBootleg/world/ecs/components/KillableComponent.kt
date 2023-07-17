package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.killable
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.ecs.api.EntityParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent

data class KillableComponent(val maxHealth: Int = 10, val health: Int = maxHealth) : EntitySavableComponent {

  val dead: Boolean = health <= 0
  val alive: Boolean = health > 0

  override fun EntityKt.Dsl.save() {
    killable = killable {
      health = this@KillableComponent.health
      maxHealth = this@KillableComponent.maxHealth
    }
  }

  companion object : EntityParentLoadableMapper<KillableComponent>() {
    var Entity.killable by propertyFor(mapper)
    var Entity.killableOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) =
      with(KillableComponent(protoEntity.killable.health, protoEntity.killable.maxHealth))

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasKillable()
  }
}
