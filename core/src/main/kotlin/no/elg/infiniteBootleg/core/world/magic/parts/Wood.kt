package no.elg.infiniteBootleg.core.world.magic.parts

import no.elg.infiniteBootleg.core.util.sealedSubclassObjectInstances
import no.elg.infiniteBootleg.core.world.magic.Description
import no.elg.infiniteBootleg.core.world.magic.MagicEffectsWithRating
import no.elg.infiniteBootleg.core.world.magic.Named
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

enum class WoodRating(val powerPercent: Double, timeToNext: Duration) {
  FRESHLY_CUT(0.3, 1.hours),
  DRIED(0.5, 2.hours),
  AGED(0.7, 5.hours),
  ANCIENT(0.9, 10.hours),
  PETRIFIED(1.0, Duration.INFINITE)
}

sealed class WoodType(val gemSlots: UInt, val ringSlots: UInt, val dryingRate: Double, val castDelay: Duration) :
  MagicEffectsWithRating<WoodRating>,
  Named,
  Description {

  companion object {

    fun valueOf(displayName: String): WoodType =
      when (displayName) {
        Birch.serializedName -> Birch
        Aerowode.serializedName -> Aerowode
        RedWood.serializedName -> RedWood
        Driftwood.serializedName -> Driftwood
        WistedWood.serializedName -> WistedWood
        Trekant.serializedName -> Trekant
        else -> throw IllegalArgumentException("Unknown wood type $displayName")
      }
  }
}

data object Birch : WoodType(1u, 0u, 1.0, 333.milliseconds) {
  override val description: String get() = "A common wood type known for its light color and smooth texture."
  override val displayName: String get() = "Birch"
}

data object Aerowode : WoodType(1u, 2u, 1.0, 250.milliseconds) {
  override val description: String get() = "A rare and lightweight wood that seems to almost float in the air."
  override val displayName: String get() = "Aerowode"
}

data object RedWood : WoodType(1u, 0u, 2.0, 150.milliseconds) {
  override val description: String get() = "A sturdy and dense wood with a deep red hue. Dries faster than most woods."
  override val displayName: String get() = "Redwood"
}

data object Driftwood : WoodType(1u, 3u, 0.25, 300.milliseconds) {
  override val description: String get() = "A wood found washed up on shores, weathered by salt and time, and so hard to dry."
  override val displayName: String get() = "Driftwood"
}

data object WistedWood : WoodType(2u, 1u, 1.0, 100.milliseconds) {
  override val description: String get() = "A twisted and gnarled wood, known for its strength and resilience."
  override val displayName: String get() = "Wistedwood"
}

data object Trekant : WoodType(3u, 2u, 1.0, 50.milliseconds) {
  override val description: String get() = "A triangular cross-section wood, prized for its unique shape and magical properties."
  override val displayName: String get() = "Trekant"
}
