package no.elg.infiniteBootleg.core.world.blocks

import com.badlogic.gdx.graphics.Color
import no.elg.infiniteBootleg.core.world.blocks.BlockLight.Companion.COMPLETE_DARKNESS
import no.elg.infiniteBootleg.core.world.blocks.BlockLight.Companion.LIGHT_RESOLUTION_SQUARE

data class LightMap(
  /** red channel */
  val r: BrightnessArray = fullyDark(),
  /** green channel */
  val g: BrightnessArray = fullyDark(),
  /** blue channel */
  val b: BrightnessArray = fullyDark(),
  /** intensity channel (alpha?) */
  val i: BrightnessArray = fullyDark()
) {

  fun averageBrightness() = i.sum() / LIGHT_RESOLUTION_SQUARE

  fun updateColor(lightMapIndex: Int, intensity: Float, tint: Color, firstTime: Boolean) {
    val rIntensity = intensity * tint.r
    val gIntensity = intensity * tint.g
    val bIntensity = intensity * tint.b

    if (firstTime) {
      r[lightMapIndex] = rIntensity
      g[lightMapIndex] = gIntensity
      b[lightMapIndex] = bIntensity
      i[lightMapIndex] = intensity
    } else {
      if (i[lightMapIndex] < intensity) {
        i[lightMapIndex] = intensity
      }
      if (r[lightMapIndex] < rIntensity) {
        r[lightMapIndex] = rIntensity
      }
      if (g[lightMapIndex] < gIntensity) {
        g[lightMapIndex] = gIntensity
      }
      if (b[lightMapIndex] < bIntensity) {
        b[lightMapIndex] = bIntensity
      }
    }
  }

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
