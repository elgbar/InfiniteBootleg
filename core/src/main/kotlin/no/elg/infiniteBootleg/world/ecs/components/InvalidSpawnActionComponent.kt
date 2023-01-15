package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction

data class InvalidSpawnActionComponent(val invalidSpawnAction: InvalidSpawnAction) : Component {
  companion object : Mapper<InvalidSpawnActionComponent>() {

    val Entity.invalidSpawnAction: InvalidSpawnAction
      get() {
        return invalidSpawnActionOrNull?.invalidSpawnAction ?: DEFAULT_ACTION
      }

    private var Entity.invalidSpawnActionOrNull by optionalPropertyFor(mapper)
    private val DEFAULT_ACTION = InvalidSpawnAction.PUSH_UP
  }
}
