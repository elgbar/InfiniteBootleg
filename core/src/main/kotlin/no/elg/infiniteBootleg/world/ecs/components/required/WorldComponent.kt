package no.elg.infiniteBootleg.world.ecs.components.required

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.LoadableMapper
import no.elg.infiniteBootleg.world.world.World

data class WorldComponent(val world: World) : EntitySavableComponent {

  companion object : LoadableMapper<WorldComponent, ProtoWorld.Entity, World>() {
    val Entity.world get() = worldComponent.world
    val Entity.worldComponent by propertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity, state: World): WorldComponent = with(WorldComponent(state))
    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = true
  }

  override fun EntityKt.Dsl.save() {
    // TODO Is there any point in this being a EntitySavableComponent?
  }
}
