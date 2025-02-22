package no.elg.infiniteBootleg.server.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.DebuggableComponent
import no.elg.infiniteBootleg.core.world.render.ServerClientChunksInView

data class ServerClientChunksInViewComponent(val serverClientChunksInView: ServerClientChunksInView) : DebuggableComponent {

  override fun hudDebug(): String = "Chunks in view: $serverClientChunksInView"

  companion object : Mapper<ServerClientChunksInViewComponent>() {
    var Entity.serverClientChunksInViewComponent: ServerClientChunksInViewComponent by propertyFor(mapper)
    val Entity.chunksInView: ServerClientChunksInView get() = serverClientChunksInViewComponent.serverClientChunksInView
  }
}
