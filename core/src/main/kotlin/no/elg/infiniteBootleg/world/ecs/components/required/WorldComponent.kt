package no.elg.infiniteBootleg.world.ecs.components.required

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.LoadableMapper
import no.elg.infiniteBootleg.world.world.World

data class WorldComponent(val world: World) : EntitySavableComponent {

  companion object : LoadableMapper<WorldComponent, ProtoWorld.Entity, World>() {
    val Entity.world get() = worldComponent.world
    val Entity.worldComponent by propertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity, state: World): WorldComponent? = safeWith { (.+) }
    override fun ProtoWorld.Entity.checkShouldLoad(state: () -> World): Boolean =
      (worldUUID == state().uuid).also { if (!it) Main.logger().error("Loaded entity in wrong world! Expected $worldUUID, but got ${state().uuid}") }
  }

  override fun EntityKt.Dsl.save() {
    this.worldUUID = world.uuid
  }
}
