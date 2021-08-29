package no.elg.infiniteBootleg.util;

import static no.elg.infiniteBootleg.world.render.WorldRender.LIGHT_LOCK;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Pool;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import org.jetbrains.annotations.NotNull;

/**
 * A pool for static xray point lights
 *
 * @author Elg
 */
public final class PointLightPool extends Pool<PointLight> {

    public static final PointLightPool inst = new PointLightPool();
    public static final int POINT_LIGHT_RAYS = 64;
    public static final int POINT_LIGHT_DISTANCE = 32;
    private final RayHandler rayHandler;
    private final WorldBody worldBody;

    private PointLightPool() {
        final World world = Main.inst().getWorld();
        rayHandler = world.getRender().getRayHandler();
        worldBody = world.getWorldBody();
    }

    @Override
    protected PointLight newObject() {
        PointLight light;
        synchronized (LIGHT_LOCK) {
            light = new PointLight(rayHandler, POINT_LIGHT_RAYS, Color.WHITE, POINT_LIGHT_DISTANCE, 0, 0);
        }
        reset(light);
        return light;
    }

    @NotNull
    public PointLight obtain(float worldX, float worldY) {
        var light = obtain();
        light.setPosition(worldX + worldBody.getWorldOffsetX(), worldY + worldBody.getWorldOffsetY());
        return light;
    }

    /**
     * @deprecated Use {@link #obtain(float, float)} to correctly calculate any world offset
     */
    @Override
    public PointLight obtain() {
        PointLight light = super.obtain();

        synchronized (LIGHT_LOCK) {
            light.setActive(true);
        }
        return light;
    }

    @Override
    public void free(PointLight light) {
        synchronized (LIGHT_LOCK) {
            light.setActive(false);
        }
        super.free(light);
    }

    @Override
    protected void reset(PointLight light) {
        synchronized (LIGHT_LOCK) {
            light.setStaticLight(true);
            light.setXray(false);
            light.setSoft(true);
            light.setSoftnessLength(World.POINT_LIGHT_SOFTNESS_LENGTH);
            light.setDistance(POINT_LIGHT_DISTANCE);
            light.setColor(Color.WHITE);
            light.setContactFilter(World.LIGHT_FILTER);
            light.setPosition(Float.MAX_VALUE, Float.MAX_VALUE);
        }
    }
}
