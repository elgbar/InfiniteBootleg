package no.elg.infiniteBootleg.core.world.blocks

import no.elg.infiniteBootleg.core.world.blocks.BlockLight.Companion.COMPLETE_DARKNESS
import no.elg.infiniteBootleg.core.world.blocks.BlockLight.Companion.LIGHT_RESOLUTION_SQUARE

data class LightMap(val r: BrightnessArray = fullyDark(), val g: BrightnessArray = fullyDark(), val b: BrightnessArray = fullyDark()) {

  fun averageBrightness() = (r.sum() + g.sum() + b.sum()) * (1f / (3f * LIGHT_RESOLUTION_SQUARE))

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LightMap) return false

    if (!r.contentEquals(other.r)) return false
    if (!g.contentEquals(other.g)) return false
    if (!b.contentEquals(other.b)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = r.contentHashCode()
    result = 31 * result + g.contentHashCode()
    result = 31 * result + b.contentHashCode()
    return result
  }

  companion object {

    /**
     * How bright a block is
     *
     * The range is from [BlockLight.Companion.COMPLETE_DARKNESS] to [BlockLight.Companion.FULL_BRIGHTNESS]
     */
    typealias Brightness = Float
    typealias BrightnessArray = FloatArray

    fun fullyDark() = BrightnessArray(LIGHT_RESOLUTION_SQUARE) { COMPLETE_DARKNESS }
    fun fullyBright() = BrightnessArray(LIGHT_RESOLUTION_SQUARE) { COMPLETE_DARKNESS }

    val SKYLIGHT_LIGHT_MAP: LightMap = LightMap(fullyBright(), fullyBright(), fullyBright())
    val NO_LIGHTS_LIGHT_MAP: LightMap = LightMap()
  }
}
