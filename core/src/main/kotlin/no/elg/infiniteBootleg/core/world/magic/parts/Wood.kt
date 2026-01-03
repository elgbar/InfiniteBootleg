@file:Suppress("unused")

package no.elg.infiniteBootleg.core.world.magic.parts

import no.elg.infiniteBootleg.core.util.sealedSubclassObjectInstances
import no.elg.infiniteBootleg.core.util.toTitleCase
import no.elg.infiniteBootleg.core.world.magic.Description
import no.elg.infiniteBootleg.core.world.magic.MagicEffectsWithRating
import no.elg.infiniteBootleg.core.world.magic.Named
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

enum class WoodRating(val powerPercent: Double, timeToNext: Duration) : Named {
  FRESHLY_CUT(1.0, 1.hours),
  DRIED(1.2, 2.hours),
  AGED(1.4, 5.hours),
  ANCIENT(1.6, 4.hours),
  PETRIFIED(1.7, Duration.INFINITE);

  override val displayName: String = name.toTitleCase()
}

sealed class WoodType(val gemSlots: UInt, val ringSlots: UInt, val dryingRate: Double, val castDelay: Duration) :
  MagicEffectsWithRating<WoodRating>,
  Named,
  Description {

  companion object {
    val woodTypes: List<WoodType> by lazy { sealedSubclassObjectInstances<WoodType>() }

    fun valueOf(displayName: String): WoodType =
      requireNotNull(woodTypes.find { it.serializedName == displayName }) {
        "Unknown wood type $displayName"
      }
  }
}

data object Birch : WoodType(1u, 0u, 1.0, 1000.milliseconds) {
  override val description: String get() = "A weakly magical, but common, wood type known for its light color."
  override val displayName: String get() = "Birch"
}

data object HangingBirch : WoodType(1u, 1u, 1.0, 1000.milliseconds) {
  override val description: String get() = "A slightly more magical wood to the common birch"
  override val displayName: String get() = "Hanging Birch"
}

// TODO on equip: player's gravity is reduced by 20% * rating.powerPercent
// TODO Imcompatible with `Lead` ring
data object Aerowode : WoodType(1u, 0u, 1.0, 750.milliseconds) {
  override val description: String get() = "A rare and lightweight wood that seems to almost float in the air"
  override val displayName: String get() = "Aerowode"
}

// TODO make it start random fires on spell land
data object RedWood : WoodType(1u, 0u, 2.0, 500.milliseconds) {
  override val description: String get() = "The wood crackles and smokes, making it dry very quickly"
  override val displayName: String get() = "Redwood"
}

// todo make it work under water
data object Driftwood : WoodType(1u, 3u, 0.25, 900.milliseconds) {
  override val description: String get() = "Even when dried, drops of water forms around its base"
  override val displayName: String get() = "Driftwood"
}

data object WistedWood : WoodType(2u, 1u, 1.0, 300.milliseconds) {
  override val description: String get() = "A magical twisted and gnarled wood, known for its ring capacity and speed"
  override val displayName: String get() = "Wistedwood"
}

data object Trekant : WoodType(3u, 2u, 1.0, 300.milliseconds) {
  override val description: String get() = "A legendary triangular cross-section wood, prized for its unique shape and magical properties."
  override val displayName: String get() = "Trekant"
}
