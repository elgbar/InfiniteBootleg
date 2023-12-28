package no.elg.infiniteBootleg.world.magic.parts

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dOrNull
import no.elg.infiniteBootleg.world.magic.Equippable
import no.elg.infiniteBootleg.world.magic.MagicEffectsWithRating
import no.elg.infiniteBootleg.world.magic.MutableSpellState
import no.elg.infiniteBootleg.world.magic.Named

enum class RingRating(val effectPercent: Double) {
  FLAWLESS(1.6),
  MINORLY_SCRATCHED(1.5),
  MAJORLY_SCRATCHED(1.45),
  MINORLY_CHIPPED(1.40),
  MAJORLY_CHIPPED(1.35),
  LARGE_FRAGMENT(1.25),
  SMALL_FRAGMENT(1.15),
  SOME_PIECES(1.10),
  SMALL_PIECES(1.05),
  DUST(1.01)
}

sealed interface RingType<in R : RingRating?> : Named, Equippable, MagicEffectsWithRating<R> {
  companion object {

    fun valueOf(displayName: String): RingType<RingRating?> {
      @Suppress("UNCHECKED_CAST")
      return when (displayName) {
        AntiGravityRing.serializedName -> AntiGravityRing
        OpalRing.serializedName -> OpalRing
        EmeraldRing.serializedName -> EmeraldRing
        RubyRing.serializedName -> RubyRing
        SapphireRing.serializedName -> SapphireRing
        else -> throw IllegalArgumentException("Unknown ring type $displayName")
      } as RingType<RingRating?>
    }
  }
}

sealed interface RatelessRingType : Named, RingType<RingRating?>
sealed interface RatedRingType : Named, RingType<RingRating>

// data object Palantir : RatelessRingType {
//  override val displayName: String = "Palantir"
//
// }

data object AntiGravityRing : RatelessRingType {
  override val displayName: String = "Anti Gravity"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating?) {
    state.entityModifications += { spell: Entity -> spell.box2dOrNull?.disableGravity() }
  }
}

data object OpalRing : RatedRingType {
  override val displayName: String = "Opal"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    state.gemPower *= rating.effectPercent
  }
}

data object EmeraldRing : RatedRingType {
  override val displayName: String = "Emerald"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    state.spellRange *= rating.effectPercent
  }
}

data object RubyRing : RatedRingType {
  override val displayName: String = "Ruby"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    state.castDelay /= rating.effectPercent
  }
}

data object SapphireRing : RatedRingType {
  override val displayName: String = "Sapphire"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    state.spellVelocity *= rating.effectPercent
  }
}

// data object AeromirRing : RatedRingType {
//  override val displayName: String = "Aeromir"
//
//  override fun onEquip(entity: Entity) {
//    //TODO
//  }
//  override fun onUnEquip(entity: Entity) {
//    //TODO
//  }
// }
//
// data object LysRing : RatedRingType {
//  override val displayName: String = "Lys"
//
//  override fun onEquip(entity: Entity) {
//    //TODO
//  }
//  override fun onUnEquip(entity: Entity) {
//    //TODO
//  }
// }
