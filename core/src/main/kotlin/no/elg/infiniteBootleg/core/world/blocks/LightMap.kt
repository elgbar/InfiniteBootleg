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

  fun averageBrightness() = maxOf(r.sum() / LIGHT_RESOLUTION_SQUARE, g.sum() / LIGHT_RESOLUTION_SQUARE, b.sum() / LIGHT_RESOLUTION_SQUARE).coerceIn(0f, 1f)

  fun updateColor(lightMapIndex: Int, intensity: Float, tint: Color) {
    val rIntensity = intensity * tint.r
    val gIntensity = intensity * tint.g
    val bIntensity = intensity * tint.b

    i[lightMapIndex] += intensity
    r[lightMapIndex] += rIntensity
    g[lightMapIndex] += gIntensity
    b[lightMapIndex] += bIntensity
  }

  inline fun reinhard(inp: Float, lumen: Float = 1f): Float = inp / (lumen + inp)
  fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)

  fun reinhardJodie(v: Float, l: Float): Float {
    val tv = reinhard(v)
    return lerp(v / (1f + l), tv, tv)
  }

  fun luminance(rHDR: Float, gHDR: Float, bHDR: Float) = rHDR * 0.2126f + gHDR * 0.7152f + bHDR * 0.0722f

  fun calculateReinhardToneMapping() {
    for (lightMapIndex in 0 until LIGHT_RESOLUTION_SQUARE) {
      val rHDR = r[lightMapIndex]
      val gHDR = g[lightMapIndex]
      val bHDR = b[lightMapIndex]

      r[lightMapIndex] = reinhard(rHDR)
      g[lightMapIndex] = reinhard(gHDR)
      b[lightMapIndex] = reinhard(bHDR)
    }
  }

  fun calculateReinhardJodieToneMapping() {
    for (lightMapIndex in 0 until LIGHT_RESOLUTION_SQUARE) {
      val rHDR = r[lightMapIndex]
      val gHDR = g[lightMapIndex]
      val bHDR = b[lightMapIndex]

      val l = i[lightMapIndex]

      r[lightMapIndex] = reinhardJodie(rHDR, l)
      g[lightMapIndex] = reinhardJodie(gHDR, l)
      b[lightMapIndex] = reinhardJodie(bHDR, l)
    }
  }

  fun calculateReinhardJodieToneMappingLuminanceByColor() {
    for (lightMapIndex in 0 until LIGHT_RESOLUTION_SQUARE) {
      val rHDR = r[lightMapIndex]
      val gHDR = g[lightMapIndex]
      val bHDR = b[lightMapIndex]

      val l = luminance(rHDR, gHDR, bHDR)

      r[lightMapIndex] = reinhardJodie(rHDR, l)
      g[lightMapIndex] = reinhardJodie(gHDR, l)
      b[lightMapIndex] = reinhardJodie(bHDR, l)
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
