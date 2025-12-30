package no.elg.infiniteBootleg.client.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.core.util.WorldCoordFloat
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.DebuggableComponent
import no.elg.infiniteBootleg.protobuf.ProtoWorld

class LastServerPositionComponent(var worldX: WorldCoordFloat, var worldY: WorldCoordFloat) : DebuggableComponent {
  override fun hudDebug(): String = "LastServerPositionComponent pos ${stringifyCompactLoc(worldX, worldY)}"

  companion object : Mapper<LastServerPositionComponent>() {
    var Entity.lastServerPositionComponentOrNull by optionalPropertyFor(mapper)
    fun Entity.setLastServerPosition(serverPos: ProtoWorld.Vector2f) {
      val lastServerPositionComponent = lastServerPositionComponentOrNull
      if (lastServerPositionComponent == null) {
        lastServerPositionComponentOrNull = LastServerPositionComponent(serverPos.x, serverPos.y)
      } else {
        lastServerPositionComponent.worldX = serverPos.x
        lastServerPositionComponent.worldY = serverPos.y
      }
    }
  }
}
