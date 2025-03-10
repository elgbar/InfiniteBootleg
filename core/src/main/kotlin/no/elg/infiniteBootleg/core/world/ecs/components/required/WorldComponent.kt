package no.elg.infiniteBootleg.core.world.ecs.components.required

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.ashley.EngineEntity
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.LoadableMapper
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld

private val logger = KotlinLogging.logger {}

data class WorldComponent(val world: World) : EntitySavableComponent {

  override fun hudDebug(): String = world.toString()

  companion object : LoadableMapper<WorldComponent, ProtoWorld.Entity, World>() {
    val Entity.world get() = worldComponent.world
    val Entity.worldComponent by propertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity, state: World): WorldComponent? = safeWith { WorldComponent(state) }
    override fun ProtoWorld.Entity.checkShouldLoad(state: () -> World): Boolean =
      (worldUUID == state().uuid).also { if (!it) logger.error { "Loaded entity in wrong world! Expected $worldUUID, but got ${state().uuid}" } }
  }

  override fun EntityKt.Dsl.save() {
    this.worldUUID = world.uuid
  }
}
