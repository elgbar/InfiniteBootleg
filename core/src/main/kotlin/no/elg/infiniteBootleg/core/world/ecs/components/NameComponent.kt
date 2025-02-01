package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld

data class NameComponent(val name: String) : EntitySavableComponent {

  override fun EntityKt.Dsl.save() {
    name = this@NameComponent.name
  }

  override fun hudDebug(): String = name

  companion object : EntityLoadableMapper<NameComponent>() {
    val Entity.name get() = nameComponent.name
    val Entity.nameOrNull get() = nameComponentOrNull?.name
    var Entity.nameComponent by propertyFor(mapper)
    var Entity.nameComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = safeWith { NameComponent(protoEntity.name) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasName()
  }
}
