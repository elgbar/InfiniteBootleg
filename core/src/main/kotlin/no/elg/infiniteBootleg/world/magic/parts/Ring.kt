package no.elg.infiniteBootleg.world.magic.parts

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dOrNull
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.magic.Equippable
import no.elg.infiniteBootleg.world.magic.MagicEffectsWithRating
import no.elg.infiniteBootleg.world.magic.MutableSpellState
import no.elg.infiniteBootleg.world.magic.Named
import kotlin.collections.plusAssign

private val logger = KotlinLogging.logger {}

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

    fun valueOf(serializedName: String): RingType<RingRating?>? {
      @Suppress("UNCHECKED_CAST")
      return when (serializedName) {
        GravityRing.serializedName -> GravityRing
        PowerRing.serializedName -> PowerRing
        SpellRangeRing.serializedName -> SpellRangeRing
        IncarnationSpeedRing.serializedName -> IncarnationSpeedRing
        SpellSpeedRing.serializedName -> SpellSpeedRing
        SpellLightRing.serializedName -> SpellLightRing
        else -> {
          logger.error { "Failed to parse ring type '$serializedName', it will be absent" }
          null
        }
      } as RingType<RingRating?>?
    }
  }
}

sealed interface RatelessRingType : Named, RingType<RingRating?>
sealed interface RatedRingType : Named, RingType<RingRating>

// data object Palantir : RatelessRingType {
//  override val displayName: String = "Palantir"
//
// }

data object GravityRing : RatelessRingType {
  override val displayName: String = "Lead"
  override val serializedName: String = "Gravity"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating?) {
    state.entityModifications += { spell: Entity -> spell.box2dOrNull?.enableGravity() }
  }
}

data object PowerRing : RatedRingType {
  override val displayName: String = "Iron power"
  override val serializedName: String = "Power"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    state.gemPower *= rating.effectPercent
  }
}

data object SpellRangeRing : RatedRingType {
  override val displayName: String = "Aluminium range"
  override val serializedName: String = "SpellRange"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    state.spellRange *= rating.effectPercent
  }
}

data object IncarnationSpeedRing : RatedRingType {
  override val displayName: String = "Incarnation Copper"
  override val serializedName: String = "IncarnationSpeed"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    state.castDelay /= rating.effectPercent
  }
}

data object SpellSpeedRing : RatedRingType {
  override val displayName: String = "Tin Speed"
  override val serializedName: String = "SpellSpeed"

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
data object SpellLightRing : RatedRingType {
  override val displayName: String = "Phosphorus Ring"
  override val serializedName: String = "SpellLight"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    // fixme Drops torhces sometimes
    state.entityModifications += { spell: Entity -> spell.safeWith { MaterialComponent(Material.TORCH) } }
  }
}
