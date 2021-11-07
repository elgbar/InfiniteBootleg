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

public class WorldLightTicker implements Ticking {

  @NotNull private final Ticker ticker;
  @NotNull private final ClientWorld world;

  private final Color tmpColor = new Color();

  WorldLightTicker(@NotNull ClientWorld world, boolean tick) {
    this.world = world;
    ticker =
        new Ticker(this, "WorldLight-" + world.getName(), tick, Settings.tps / 3, Double.MAX_VALUE);
  }

  @Override
  public void tick() {
    ClientWorldRender wr = world.getRender();
    WorldTime time = world.getWorldTime();

    if (Settings.renderLight) {
      if (Settings.dayTicking) {
        DirectionalLight skylight = wr.getSkylight();
        if (skylight != null) {
          float currTime = time.getTime();
          skylight.setDirection(currTime);
          if (time.normalizedTime() >= 180) {
            float brightness = time.getSkyBrightness(currTime);
            if (brightness > 0) {
              Color newColor =
                  tmpColor.set(time.getBaseColor()).mul(brightness, brightness, brightness, 1);
              skylight.setColor(newColor);
            } else if (!skylight.getColor().equals(Color.BLACK)) {
              skylight.setColor(Color.BLACK);
            }
          }
        }
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
