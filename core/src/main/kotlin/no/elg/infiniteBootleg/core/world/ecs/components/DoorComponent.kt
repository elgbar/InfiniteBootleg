package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.box2d.ObjectContactTracker
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld

class DoorComponent : EntitySavableComponent {

  val contacts = ObjectContactTracker<Entity>()
  val closed: Boolean get() = contacts.isEmpty

  override fun hudDebug(): String = "contacts ${contacts.size} (closed? $closed)"

  companion object : EntityLoadableMapper<DoorComponent>() {
    var Entity.doorComponent by propertyFor(mapper)
    var Entity.doorComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): DoorComponent? = safeWith { DoorComponent() }
    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasDoor()
    val PROTO_DOOR: ProtoWorld.Entity.Door = ProtoWorld.Entity.Door.getDefaultInstance()
  }

  override fun EntityKt.Dsl.save() {
    door = PROTO_DOOR
  }
}
