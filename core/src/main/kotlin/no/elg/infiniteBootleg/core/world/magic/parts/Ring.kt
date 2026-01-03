@file:Suppress("unused")

package no.elg.infiniteBootleg.core.world.magic.parts

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.util.sealedSubclassObjectInstances
import no.elg.infiniteBootleg.core.util.toTitleCase
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent
import no.elg.infiniteBootleg.core.world.magic.Description
import no.elg.infiniteBootleg.core.world.magic.Equippable
import no.elg.infiniteBootleg.core.world.magic.MagicEffectsWithRating
import no.elg.infiniteBootleg.core.world.magic.MutableSpellState
import no.elg.infiniteBootleg.core.world.magic.Named

enum class RingRating(val effectPercent: Double) : Named {
  PERFECT(1.75),
  NEARLY_PERFECT(1.6),
  MINORLY_SCRATCHED(1.5),
  MAJORLY_SCRATCHED(1.45),
  MINORLY_CHIPPED(1.40),
  MAJORLY_CHIPPED(1.35),
  LARGE_FRAGMENT(1.25),
  SMALL_FRAGMENT(1.15),
  SOME_PIECES(1.10),
  SMALL_PIECES(1.05),
  DUST(1.01);

  override val displayName: String = name.toTitleCase()
}

sealed interface RingType<in R : RingRating?> :
  Named,
  Equippable,
  MagicEffectsWithRating<R>,
  Description {
  companion object {

    val ringRatings: List<RingType<RingRating?>> by lazy { sealedSubclassObjectInstances<RingType<RingRating?>>() }

    fun valueOf(serializedName: String): RingType<RingRating?> =
      requireNotNull(ringRatings.find { it.serializedName == serializedName }) {
        "Unknown ring type $serializedName"
      }
  }
}

sealed interface RatelessRingType : RingType<RingRating?>

sealed interface RatedRingType : RingType<RingRating>

// data object Palantir : RatelessRingType {
//  override val displayName: String = "Palantir"
//
// }

data object GravityRing : RatelessRingType {
  override val displayName: String = "Lead"
  override val serializedName: String = "Gravity"
  override val description: String
    get() = "Causes spells cast with this ring to be affected by gravity."

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating?) {
    state.entityModifications += { spell: Entity -> spell.box2dOrNull?.enableGravity() }
  }
}

data object PowerRing : RatedRingType {
  override val displayName: String = "Iron power"
  override val serializedName: String = "Power"
  override val description: String
    get() = "Increases the power of the spell"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    state.gemPower *= rating.effectPercent
  }
}

data object SpellRangeRing : RatedRingType {
  override val displayName: String = "Aluminium range"
  override val serializedName: String = "SpellRange"
  override val description: String
    get() = "Increases the range of the spell"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    state.spellRange *= rating.effectPercent
  }
}

data object IncarnationSpeedRing : RatedRingType {
  override val displayName: String = "Incarnation Copper"
  override val serializedName: String = "IncarnationSpeed"
  override val description: String
    get() = "Decreases the casting time of the spell"

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating) {
    state.variableCastDelay /= rating.effectPercent
  }
}

data object SpellSpeedRing : RatedRingType {
  override val displayName: String = "Tin Speed"
  override val serializedName: String = "SpellSpeed"
  override val description: String
    get() = "Increases the speed of the spell"

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

data object SpellLightRing : RatelessRingType {
  override val displayName: String = "Phosphorus Ring"
  override val serializedName: String = "SpellLight"
  override val description: String
    get() = "Causes the spell to emit light like a torch."

  override fun onSpellCreate(state: MutableSpellState, rating: RingRating?) {
    state.entityModifications += { spell: Entity ->
      spell.safeWith { MaterialComponent(Material.Torch) }
      // Must be occupying blocks to emit light
      spell.safeWith { OccupyingBlocksComponent(hardLink = false) }
    }
  }
}
