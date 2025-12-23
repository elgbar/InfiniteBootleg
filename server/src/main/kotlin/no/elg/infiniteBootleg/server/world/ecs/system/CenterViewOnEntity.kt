package no.elg.infiniteBootleg.server.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.allOf
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.server.world.ecs.components.transients.ServerClientChunksInViewComponent
import no.elg.infiniteBootleg.server.world.ecs.components.transients.ServerClientChunksInViewComponent.Companion.chunksInView

val InViewFamily = allOf(ServerClientChunksInViewComponent::class, PositionComponent::class).get()

object CenterViewOnEntity : IteratingSystem(InViewFamily, UPDATE_PRIORITY_DEFAULT) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val (x, y) = entity.positionComponent
    entity.chunksInView.setCenter(x.worldToChunk(), y.worldToChunk())
  }
}
