package no.elg.infiniteBootleg.core.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.WorldCoordNumber
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.DebuggableComponent
import no.elg.infiniteBootleg.core.world.magic.SpellState

// The spell state of a spell entity
data class SpellStateComponent(
  val state: SpellState,
  val spawnX: WorldCoordNumber,
  val spawnY: WorldCoordNumber,
  val spawnDx: Number,
  val spawnDy: Number
) : DebuggableComponent {

  override fun hudDebug(): String =
    "state $state, spawn pos ${
      stringifyCompactLoc(
        spawnX,
        spawnY
      )
    }, spawn vel ${stringifyCompactLoc(spawnDx, spawnDy)}"

  companion object : Mapper<SpellStateComponent>() {
    var Entity.spellStateComponent: SpellStateComponent by propertyFor(mapper)
    var Entity.spellStateComponentOrNull: SpellStateComponent? by optionalPropertyFor(mapper)
    val Entity.spellStateOrNull: SpellState? get() = spellStateComponentOrNull?.state
  }
}
