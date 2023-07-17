package no.elg.infiniteBootleg.world.ecs.components.transients

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.ecs.api.ParentLoadableMapper
import no.elg.infiniteBootleg.world.world.World

data class WorldComponent(val world: World) : Component {

  companion object : ParentLoadableMapper<WorldComponent, World>() {
    val Entity.world get() = worldComponent.world
    val Entity.worldComponent by propertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: World) = with(WorldComponent(protoEntity))

    override fun World.checkShouldLoad(): Boolean = true
  }
}
