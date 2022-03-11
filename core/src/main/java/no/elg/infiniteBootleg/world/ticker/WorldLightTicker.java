package no.elg.infiniteBootleg.world.ticker;

import box2dLight.DirectionalLight;
import com.badlogic.gdx.graphics.Color;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.render.ClientWorldRender;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.time.WorldTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldLightTicker implements Ticking {

  @NotNull private final Ticker ticker;
  @NotNull private final ClientWorld world;

  private final Color tmpColor = new Color();

  WorldLightTicker(@NotNull ClientWorld world, boolean tick) {
    this.world = world;
    ticker =
        new Ticker(this, "WorldLight-" + world.getName(), tick, Settings.tps / 3, Double.MAX_VALUE);
  }

  private void updateLight(@Nullable DirectionalLight light, float direction, float intensity) {
    WorldTime time = world.getWorldTime();
    if (light != null) {
      float currTime = time.getTime();
      light.setDirection(direction);
      float brightness = time.getSkyBrightness(currTime) * intensity;
      if (brightness > 0) {
        Color newColor =
            tmpColor.set(time.getBaseColor()).mul(brightness, brightness, brightness, 1);
        light.setColor(newColor);
      } else if (!light.getColor().equals(Color.BLACK)) {
        light.setColor(Color.BLACK);
      }
    }
  }

  @Override
  public void tick() {
    ClientWorldRender wr = world.getRender();

    if (Settings.renderLight) {
      if (Settings.dayTicking) {
        updateLight(wr.getAmbientLight(), WorldTime.MIDDAY_TIME, 0.25f);
        updateLight(wr.getSun(), world.getWorldTime().getTime(), 1f);
      }
      synchronized (WorldRender.BOX2D_LOCK) {
        synchronized (WorldRender.LIGHT_LOCK) {
          wr.getRayHandler().update();
        }
      }
    }
  }

  public Ticker getTicker() {
    return ticker;
  }
}
