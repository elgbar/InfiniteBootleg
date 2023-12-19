package no.elg.infiniteBootleg.world.magic

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.world.magic.parts.GemRating
import no.elg.infiniteBootleg.world.magic.parts.GemType
import no.elg.infiniteBootleg.world.magic.parts.RingRating
import no.elg.infiniteBootleg.world.magic.parts.RingType
import no.elg.infiniteBootleg.world.magic.parts.WoodRating
import no.elg.infiniteBootleg.world.magic.parts.WoodType

data class Staff(val wood: Wood, val gems: List<Gem>, val rings: List<Ring>) : Equippable {

  fun createSpellState(entity: Entity): MutableSpellState {
    val state = MutableSpellState(
      holder = entity,
      staff = this,
      // FIXME placeholder
      spellRange = 32.0,
      castDelay = wood.type.castDelay / wood.rating.powerPercent,
      // FIXME placeholder
      gemPower = 1.0,
      // FIXME placeholder
      spellVelocity = 10.0,
      entityModifications = mutableListOf()
    )
    wood.onSpellCreate(state)
    gems.forEach { it.onSpellCreate(state) }
    rings.forEach { it.onSpellCreate(state) }
    return state
  }

  override fun onEquip(entity: Entity) {
    wood.onEquip(entity)
    gems.forEach { it.onEquip(entity) }
    rings.forEach { it.onEquip(entity) }
  }

  override fun onUnequip(entity: Entity) {
    wood.onUnequip(entity)
    gems.forEach { it.onUnequip(entity) }
    rings.forEach { it.onUnequip(entity) }
  }
}

data class Wood(val type: WoodType, val rating: WoodRating) : MagicEffects, Equippable {
  override fun onSpellCreate(state: MutableSpellState) = type.onSpellCreate(state, rating)
  override fun onSpellCast(state: SpellState, spellEntity: Entity) = type.onSpellCast(state, spellEntity, rating)
  override fun onSpellLand(state: SpellState, spellEntity: Entity) = type.onSpellLand(state, spellEntity, rating)
}

data class Gem(val type: GemType, val rating: GemRating) : MagicEffects, Equippable {
  override fun onSpellCreate(state: MutableSpellState) = type.onSpellCreate(state, rating)
  override fun onSpellCast(state: SpellState, spellEntity: Entity) = type.onSpellCast(state, spellEntity, rating)
  override fun onSpellLand(state: SpellState, spellEntity: Entity) = type.onSpellLand(state, spellEntity, rating)
}

data class Ring(val type: RingType<RingRating?>, val rating: RingRating?) : MagicEffects, Equippable {

  override fun onSpellCreate(state: MutableSpellState) = type.onSpellCreate(state, rating)
  override fun onSpellCast(state: SpellState, spellEntity: Entity) = type.onSpellCast(state, spellEntity, rating)
  override fun onSpellLand(state: SpellState, spellEntity: Entity) = type.onSpellLand(state, spellEntity, rating)
}
