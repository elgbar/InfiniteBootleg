package no.elg.infiniteBootleg.util;

import static no.elg.infiniteBootleg.world.render.WorldRender.LIGHT_LOCK;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import org.jetbrains.annotations.NotNull;

/**
 * A pool for static xray point lights
 *
 * @author Elg
 */
public final class PointLightPool extends Pool<PointLight> {

    public static final int POINT_LIGHT_RAYS = 64;
    public static final int POINT_LIGHT_DISTANCE = 32;
    private final RayHandler rayHandler;
    private final WorldBody worldBody;

    private static final ConcurrentMap<World, PointLightPool> poolMap = new ConcurrentHashMap<>();

    private PointLightPool(@NotNull World world) {
        rayHandler = world.getRender().getRayHandler();
        worldBody = world.getWorldBody();
    }

    public static PointLightPool getPool(@NotNull World world) {
        return poolMap.computeIfAbsent(world, PointLightPool::new);
    }

    public static void clearAllPools() {
        synchronized (poolMap) {
            poolMap.clear();
        }
    }

    @Override
    protected PointLight newObject() {
        PointLight light;
        synchronized (LIGHT_LOCK) {
            light = new PointLight(rayHandler, POINT_LIGHT_RAYS, Color.WHITE, POINT_LIGHT_DISTANCE, Float.MAX_VALUE, Float.MAX_VALUE);
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
    @Deprecated
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

        final Vector2 position = light.getPosition();
        synchronized (LIGHT_LOCK) {
            light.setActive(false);
        }
        super.free(light);
    }

    @Override
    protected void discard(PointLight light) {
        synchronized (LIGHT_LOCK) {
            light.remove(true);
        }
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
