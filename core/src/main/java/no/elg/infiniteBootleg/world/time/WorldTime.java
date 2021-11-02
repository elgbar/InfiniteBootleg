package no.elg.infiniteBootleg.world.time;

import com.badlogic.gdx.graphics.Color;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public class WorldTime {

  /**
   * How many degrees the time light should have before triggering sunset/sunrise. This will happen
   * from {@code -TWILIGHT_DEGREES} to {@code +TWILIGHT_DEGREES}
   */
  public static final float TWILIGHT_DEGREES = 20;

  public static final float SUNSET_TIME = -180 + TWILIGHT_DEGREES;
  public static final float SUNRISE_TIME = 0;
  public static final float MIDDAY_TIME = -90;
  public static final float MIDNIGHT_TIME = -270;
  @NotNull private final World world;
  @NotNull private final Color baseColor = new Color(Color.WHITE);
  private float time = WorldTime.MIDDAY_TIME;
  private float timeScale = 1;

  public WorldTime(World world) {
    this.world = world;
  }

  /**
   * Calculate how bright the sky should be. During the night the value will always be {@code 0},
   * during twilight (ie from {@code 360} to {@code 360-}{@link WorldTime#TWILIGHT_DEGREES} and
   * {@code 180+}{@link WorldTime#TWILIGHT_DEGREES} to {@code 180-}{@link
   * WorldTime#TWILIGHT_DEGREES}) the light will change. During daytime the value will always be 1
   *
   * <p>The time used will be the current world time ie {@link #getTime()}
   *
   * @return A brightness value between 0 and 1 (both inclusive)
   */
  public float getSkyBrightness() {
    return getSkyBrightness(time);
  }

  /**
   * Calculate how bright the sky should be. During the night the value will always be {@code 0},
   * during twilight (ie from {@code 360} to {@code 360-}{@link WorldTime#TWILIGHT_DEGREES} and
   * {@code 180+}{@link WorldTime#TWILIGHT_DEGREES} to {@code 180-}{@link
   * WorldTime#TWILIGHT_DEGREES}) the light will change. During daytime the value will always be 1
   *
   * @param time The time to calculate
   * @return A brightness value between 0 and 1 (both inclusive)
   */
  public float getSkyBrightness(float time) {
    float dir = Util.normalizedDir(time);
    float gray = 0;

    if (dir <= 180) {
      return 0;
    } else if (dir > 360 - WorldTime.TWILIGHT_DEGREES && dir < 360) {
      gray = (360 - dir) / (WorldTime.TWILIGHT_DEGREES);
    } else if (dir >= 180 && dir <= 180 + WorldTime.TWILIGHT_DEGREES) {
      gray = ((dir - 180) / (WorldTime.TWILIGHT_DEGREES));
    } else if (dir > 180) {
      gray = 1; // white
    }
    return gray;
  }

  public float normalizedTime() {
    return Util.normalizedDir(time);
  }

  public float getTimeScale() {
    return timeScale;
  }

  public void setTimeScale(float timeScale) {
    this.timeScale = timeScale;
  }

  public float getTime() {
    return time;
  }

  public void setTime(float time) {
    this.time = time;
  }

  public Color getBaseColor() {
    return baseColor;
  }

  public World getWorld() {
    return world;
  }
}
