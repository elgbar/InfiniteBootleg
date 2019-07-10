package no.elg.infiniteBootleg.util;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Pool;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.render.WorldRender;

/**
 * A pool for static xray point lights
 *
 * @author Elg
 */
public final class PointLightPool extends Pool<PointLight> {

    public static final PointLightPool inst = new PointLightPool();

    private RayHandler rayHandler;

    public static final int POINT_LIGHT_RAYS = 32;
    public static final int POINT_LIGHT_DISTANCE = 10;

    private PointLightPool() {
        rayHandler = Main.inst().getWorld().getRender().getRayHandler();
    }

    @Override
    public PointLight obtain() {
        return newObject();
    }

    @Override
    protected PointLight newObject() {
        PointLight light;
        synchronized (WorldRender.LIGHT_LOCK) {
            light = new PointLight(rayHandler, POINT_LIGHT_RAYS, new Color(1, 1, 0.9f, 1), POINT_LIGHT_DISTANCE, 0, 0);
        }
        light.setStaticLight(true);
        light.setXray(true);
        light.setSoft(false);
        return light;
    }

    @Override
    public void free(PointLight light) {
        synchronized (WorldRender.LIGHT_LOCK) {
            light.remove();
        }
    }
}
