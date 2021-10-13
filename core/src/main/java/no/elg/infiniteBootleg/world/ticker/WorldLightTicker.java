package no.elg.infiniteBootleg.world.ticker;

import box2dLight.DirectionalLight;
import com.badlogic.gdx.graphics.Color;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.time.WorldTime;
import org.jetbrains.annotations.NotNull;

public class WorldLightTicker implements Ticking {

    @NotNull
    private final Ticker ticker;
    @NotNull
    private final World world;
    private final float timeChangePerTick;

    private final Color tmpColor = new Color();

    WorldLightTicker(@NotNull World world, boolean tick) {
        this.world = world;
        ticker = new Ticker(this, "WorldLight-" + world.getName(), tick, Settings.tps / 3, Double.MAX_VALUE);

        timeChangePerTick = ticker.getSecondsDelayBetweenTicks() / 10;
    }

    @Override
    public void tick() {
        WorldRender wr = world.getRender();
        WorldTime time = world.getWorldTime();
        //update light direction
        if (Settings.dayTicking) {
            time.setTime(time.getTime() - timeChangePerTick * time.getTimeScale());
            if (Settings.renderGraphic) {

                DirectionalLight skylight = wr.getSkylight();
                float currTime = time.getTime();
                if (time.normalizedTime() >= 180) {
                    skylight.setDirection(currTime);
                }
                float brightness = time.getSkyBrightness(currTime);
                if (brightness > 0) {
                    Color newColor = tmpColor.set(time.getBaseColor()).mul(brightness, brightness, brightness, 1);
                    skylight.setColor(newColor);
                }
                else if (!skylight.getColor().equals(Color.BLACK)) {
                    skylight.setColor(Color.BLACK);
                }
            }
        }

        if (Settings.renderGraphic && Settings.renderLight) {
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
