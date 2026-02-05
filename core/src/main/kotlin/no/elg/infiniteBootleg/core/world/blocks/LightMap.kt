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
  /** intensity channel - distance to closest light source (alpha?) */
  val i: BrightnessArray = fullyDark()
) {

  fun averageBrightness() = i.sum() / LIGHT_RESOLUTION_SQUARE

  fun updateColor(lightMapIndex: Int, intensity: Float, tint: Color) {
    val rIntensity = intensity * tint.r
    val gIntensity = intensity * tint.g
    val bIntensity = intensity * tint.b

    if (i[lightMapIndex] < intensity) {
      i[lightMapIndex] = intensity
    }
    r[lightMapIndex] += rIntensity
    g[lightMapIndex] += gIntensity
    b[lightMapIndex] += bIntensity
  }

  fun calculateReinhardToneMapping() {
    for (lightMapIndex in 0 until LIGHT_RESOLUTION_SQUARE) {
      val rHDR = r[lightMapIndex]
      val gHDR = g[lightMapIndex]
      val bHDR = b[lightMapIndex]

      // Reinhard tone mapping: x / (1 + x)
      // This compresses bright values while preserving color ratios
      r[lightMapIndex] = rHDR / (1f + rHDR)
      g[lightMapIndex] = gHDR / (1f + gHDR)
      b[lightMapIndex] = bHDR / (1f + bHDR)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LightMap) return false

    if (!r.contentEquals(other.r)) return false
    if (!g.contentEquals(other.g)) return false
    if (!b.contentEquals(other.b)) return false
    if (!i.contentEquals(other.i)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = r.contentHashCode()
    result = 31 * result + g.contentHashCode()
    result = 31 * result + b.contentHashCode()
    result = 31 * result + i.contentHashCode()
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
