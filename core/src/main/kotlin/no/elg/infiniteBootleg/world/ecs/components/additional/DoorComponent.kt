package no.elg.infiniteBootleg.world.ecs.components.additional

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.box2d.ObjectContactTracker
import no.elg.infiniteBootleg.world.ecs.api.AdditionalComponentsSavableComponent
import no.elg.infiniteBootleg.world.ecs.api.StatelessAdditionalComponentsLoadableMapper

class DoorComponent : AdditionalComponentsSavableComponent {

  val contacts = ObjectContactTracker<Entity>()
  val closed: Boolean get() = contacts.isEmpty

  companion object : StatelessAdditionalComponentsLoadableMapper<DoorComponent>() {
    var Entity.doorComponent by propertyFor(mapper)
    var Entity.doorComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity.AdditionalComponents): DoorComponent = with(DoorComponent())
    override fun ProtoWorld.Entity.AdditionalComponents.checkShouldLoad(): Boolean = hasDoor()
  }

  override fun EntityKt.AdditionalComponentsKt.Dsl.save() {
    door = true
  }
}
