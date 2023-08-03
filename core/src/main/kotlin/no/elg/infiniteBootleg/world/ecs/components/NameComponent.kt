package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent

data class NameComponent(val name: String) : EntitySavableComponent {

  override fun EntityKt.Dsl.save() {
    name = this@NameComponent.name
  }

  companion object : EntityLoadableMapper<NameComponent>() {
    val Entity.name get() = nameComponent.name
    val Entity.nameOrNull get() = nameComponentOrNull?.name
    var Entity.nameComponent by propertyFor(mapper)
    var Entity.nameComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = with(NameComponent(protoEntity.name))

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasName()
  }
}
