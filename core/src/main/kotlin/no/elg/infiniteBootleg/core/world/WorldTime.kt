package no.elg.infiniteBootleg.core.world

import com.badlogic.gdx.graphics.Color
import no.elg.infiniteBootleg.core.util.Util
import no.elg.infiniteBootleg.core.world.world.World

class WorldTime(val world: World) {
  val baseColor = Color(Color.WHITE)

  @Volatile
  var time = SUNRISE_TIME

  @Volatile
  var timeScale = 1f

  /**
   * Calculate how bright the sky should be. During the night the value will always be `0`,
   * during twilight (ie from `360` to `360-`[WorldTime.TWILIGHT_DEGREES] and
   * `180+`[WorldTime.TWILIGHT_DEGREES] to `180-`[WorldTime.TWILIGHT_DEGREES]) the light will change. During daytime the value will always be 1
   *
   *
   * The time used will be the current world time ie [getTime]
   *
   * @return A brightness value between 0 and 1 (both inclusive)
   */
  val skyBrightness: Float
    get() = getSkyBrightness(time)

  /**
   * Calculate how bright the sky should be. During the night the value will always be `0`,
   * during twilight (ie from `360` to `360-`[WorldTime.TWILIGHT_DEGREES] and
   * `180+`[WorldTime.TWILIGHT_DEGREES] to `180-`[WorldTime.TWILIGHT_DEGREES]) the light will change. During daytime the value will always be 1
   *
   * @param time The time to calculate
   * @return A brightness value between 0 and 1 (both inclusive)
   */
  fun getSkyBrightness(time: Float = this.time): Float =
    atTime(
      time = time,
      day = { 1f },
      dusk = { 1f - (it - SUNSET_TIME) / (DUSK_TIME - SUNSET_TIME) },
      night = { 0f },
      dawn = { (it - DAWN_TIME) / (SUNRISE_TIME - DAWN_TIME) }
    )

  fun timeOfDay(time: Float = this.time): String =
    atTime(
      time = time,
      day = { "Day" },
      dusk = { "Dusk" },
      night = { "Night" },
      dawn = { "Dawn" }
    )

  inline fun <T> atTime(
    time: Float = this.time,
    day: (Float) -> T,
    dusk: (Float) -> T,
    night: (Float) -> T,
    dawn: (Float) -> T
  ): T {
    val dir = Util.normalizedDir(time)
    return when {
      // The sun has not set yet
      dir < SUNSET_TIME -> day(dir)
      // As the sun sinks the ambient light fades
      dir in SUNSET_TIME..DUSK_TIME -> dusk(dir)
      // Night between dusk and dawn
      dir in DUSK_TIME..DAWN_TIME -> night(dir)
      // As the sun returns the ambient light increases
      dir in DAWN_TIME..SUNRISE_TIME -> dawn(dir)
      // Day between sunrise and sunset, no need for an explicit check here
      else -> day(dir)
    }
  }

  fun normalizedTime(): Float = Util.normalizedDir(time)

  companion object {
    /**
     * How many degrees the time light should have before triggering sunset/sunrise. This will happen
     * from `-TWILIGHT_DEGREES` to `+TWILIGHT_DEGREES`
     */
    const val TWILIGHT_DEGREES = 18f

    /**
     * The moment the sun is parallel to the horizon and is rising
     */
    const val SUNRISE_TIME = 180 - TWILIGHT_DEGREES

    /**
     * When the world begins to lighten up
     */
    const val DAWN_TIME = SUNRISE_TIME - TWILIGHT_DEGREES

    /**
     * The moment the sun is parallel to the horizon and is sinking
     */
    const val SUNSET_TIME = 0 + TWILIGHT_DEGREES

    /**
     * When there is no more ambient light from the sun
     */
    const val DUSK_TIME = SUNSET_TIME + TWILIGHT_DEGREES

    /**
     * Middle of the day
     */
    const val MIDDAY_TIME = 270f

    /**
     * Middle of the night
     */
    const val MIDNIGHT_TIME = 90f
  }
}
