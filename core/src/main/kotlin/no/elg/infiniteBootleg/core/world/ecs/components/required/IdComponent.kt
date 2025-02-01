package no.elg.infiniteBootleg.core.world.ecs.components.required

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.util.toProtoEntityRef
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import java.util.UUID

data class IdComponent(val id: String = UUID.randomUUID().toString()) : EntitySavableComponent {

  override fun EntityKt.Dsl.save() {
    ref = id.toProtoEntityRef()
  }

  override fun hudDebug(): String = id

  companion object : EntityLoadableMapper<IdComponent>() {

    fun createRandomId(): IdComponent = IdComponent(UUID.randomUUID().toString())

    val Entity.id get() = idComponent.id
    val Entity.idComponent by propertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = safeWith { IdComponent(protoEntity.ref.id) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = true
  }
}
