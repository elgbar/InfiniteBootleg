package no.elg.infiniteBootleg.world.ecs.components.transients

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.world.magic.SpellState

// Mark a spell as the last spell cast by an entity
class LastSpellCastComponent(val state: SpellState) : Component {
  companion object : Mapper<LastSpellCastComponent>() {
    var Entity.lastSpellCastComponentOrNull: LastSpellCastComponent? by optionalPropertyFor(LastSpellCastComponent.mapper)
    val Entity.lastSpellCastOrNull: SpellState? get() = lastSpellCastComponentOrNull?.state
  }
}
