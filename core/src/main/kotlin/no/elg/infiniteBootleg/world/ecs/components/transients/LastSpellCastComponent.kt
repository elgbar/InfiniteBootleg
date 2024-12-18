package no.elg.infiniteBootleg.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.world.ecs.api.restriction.component.DebuggableComponent
import no.elg.infiniteBootleg.world.magic.SpellState

// Mark a spell as the last spell cast by an entity
data class LastSpellCastComponent(val state: SpellState) : DebuggableComponent {

  override fun hudDebug(): String = "last state: $state"

  companion object : Mapper<LastSpellCastComponent>() {
    var Entity.lastSpellCastComponentOrNull: LastSpellCastComponent? by optionalPropertyFor(mapper)
    val Entity.lastSpellCastOrNull: SpellState? get() = lastSpellCastComponentOrNull?.state
  }
}
