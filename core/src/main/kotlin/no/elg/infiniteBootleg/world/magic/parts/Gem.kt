package no.elg.infiniteBootleg.world.magic.parts

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.util.breakableLocs
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.magic.MagicEffectsWithRating
import no.elg.infiniteBootleg.world.magic.Named
import no.elg.infiniteBootleg.world.magic.SpellState

enum class GemRating(val powerPercent: Double) {
  FLAWLESS(1.0),
  SCRATCHED(0.85),
  CHIPPED(0.7),
  CRACKED(0.5),
  FRACTURED(0.25),
  RUINED(0.0)
}

sealed interface GemType : Named, MagicEffectsWithRating<GemRating>

data object Diamond : GemType {
  override val displayName: String = "Diamond"
  const val FULL_POWER = 10 // blocks radius

  override fun onSpellLand(state: SpellState, spellEntity: Entity, rating: GemRating) {
    val breakRadius = (FULL_POWER * state.gemPower * rating.powerPercent).coerceAtLeast(1.0)
    val world = spellEntity.world
    val pos = spellEntity.positionComponent

    val breakableBlocks = spellEntity.breakableLocs(world, pos.blockX, pos.blockY, breakRadius.toFloat(), state.spellRange.toFloat()).asIterable()
    world.removeBlocks(breakableBlocks)
  }
}
