package no.elg.infiniteBootleg.world.ecs.components.transients

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.world.magic.SpellState

class SpellStateComponent(val state: SpellState) : Component {
  companion object : Mapper<SpellStateComponent>() {
    var Entity.spellStateComponent: SpellStateComponent by propertyFor(SpellStateComponent.mapper)
    var Entity.spellStateComponentOrNull: SpellStateComponent? by optionalPropertyFor(SpellStateComponent.mapper)
    val Entity.spellStateOrNull: SpellState? get() = spellStateComponentOrNull?.state
    val Entity.spellState: SpellState get() = spellStateComponent.state
  }
}
