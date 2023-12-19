package no.elg.infiniteBootleg.world.magic

import com.badlogic.ashley.core.Entity

interface MagicEffects {
  /**
   * Called when a spell is created, usually just after the previous spell has been cast
   */
  fun onSpellCreate(state: MutableSpellState): Unit = Unit

  /**
   * Called when a spell is cast. The state cannot be mutated at this point
   */
  fun onSpellCast(state: SpellState, spellEntity: Entity): Unit = Unit

  /**
   * Called when a spell is lands. The state cannot be mutated at this point
   */
  fun onSpellLand(state: SpellState, spellEntity: Entity): Unit = Unit
}

interface MagicEffectsWithRating<in E> {
  /**
   * Called when a spell is created, usually just after the previous spell has been cast
   */
  fun onSpellCreate(state: MutableSpellState, rating: E): Unit = Unit

  /**
   * Called when a spell is cast. The state cannot be mutated at this point
   */
  fun onSpellCast(state: SpellState, spellEntity: Entity, rating: E): Unit = Unit

  /**
   * Called when a spell is lands. The state cannot be mutated at this point
   */
  fun onSpellLand(state: SpellState, spellEntity: Entity, rating: E): Unit = Unit
}
