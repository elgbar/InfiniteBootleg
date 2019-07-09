package no.elg.infiniteBootleg.util;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Pool;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.render.WorldRender;
import org.jetbrains.annotations.NotNull;

/**
 * A pool for static xray point lights
 *
 * @author Elg
 */
public final class PointLightPool extends Pool<PointLight> {

    public static final PointLightPool inst = new PointLightPool();

    private RayHandler rayHandler;
    private boolean updateRayHandler = true;

    public static final int POINT_LIGHT_RAYS = 32;
    public static final int POINT_LIGHT_DISTANCE = 10;

    private PointLightPool() {}

    /**
     * Clear the pool and use the new rayhandler
     */
    public void updateRayHandler() {
        clear();
        updateRayHandler = true;
    }

    @Override
    public PointLight obtain() {
        if (updateRayHandler) {
            rayHandler = Main.inst().getWorld().getRender().getRayHandler();
            updateRayHandler = false;
        }
        PointLight light = super.obtain();
        if (light == null) {
            Gdx.app.log("PointLightPool", "obtain returned null light");
            light = newObject();
        }
        synchronized (WorldRender.LIGHT_LOCK) {
            light.setActive(true);
        }
        return light;
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
    public void clear() {
        for (int i = 0, free = getFree(); i < free; ++i) {
            obtain().remove(true);
        }
        super.clear();
    }

    @Override
    protected void reset(@NotNull PointLight light) {
        //FIXME reset rayhandler for those lights who has a different one than this
        synchronized (WorldRender.LIGHT_LOCK) {
            light.setActive(false);
        }
    }
}
