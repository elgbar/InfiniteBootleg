package no.elg.infiniteBootleg.light;

import static no.elg.infiniteBootleg.world.GlobalLockKt.BOX2D_LOCK;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A pool for static xray point lights
 *
 * @author Elg
 */
public final class PointLightPool extends Pool<PointLight> implements Disposable {

  public static final int POINT_LIGHT_RAYS = 64;
  public static final int POINT_LIGHT_DISTANCE = 32;
  private final RayHandler rayHandler;
  private final WorldBody worldBody;

  private static final ConcurrentMap<ClientWorld, PointLightPool> poolMap = new ConcurrentHashMap<>();

  private PointLightPool(@NotNull ClientWorld world) {
    rayHandler = world.getRender().getRayHandler();
    worldBody = world.getWorldBody();
  }

  @Nullable
  public static PointLightPool getPool(@NotNull World world) {
    if (world instanceof ClientWorld clientWorld) {
      return getPool(clientWorld);
    }
    return null;
  }

  @NotNull
  public static PointLightPool getPool(@NotNull ClientWorld world) {
    return poolMap.computeIfAbsent(world, PointLightPool::new);
  }

  public static void clearAllPools() {
    synchronized (poolMap) {
      for (PointLightPool pool : poolMap.values()) {
        pool.dispose();
      }
      poolMap.clear();
    }
  }

  /**
   * Must be called within {@link WorldBody#postBox2dRunnable(Runnable)} or {@link  World#postBox2dRunnable(Runnable)}
   */
  @Override
  protected PointLight newObject() {
    PointLight light;
    synchronized (BOX2D_LOCK) {
      light =
          new PointLight(
              rayHandler,
              POINT_LIGHT_RAYS,
              Color.WHITE,
              POINT_LIGHT_DISTANCE,
              Float.MAX_VALUE,
              Float.MAX_VALUE);
      reset(light);
    }
    return light;
  }

  /**
   * Must be called within {@link WorldBody#postBox2dRunnable(Runnable)} or {@link  World#postBox2dRunnable(Runnable)}
   */
  @NotNull
  public PointLight obtain(float worldX, float worldY) {
    synchronized (BOX2D_LOCK) {
      var light = obtain();
      light.setPosition(worldX + worldBody.getWorldOffsetX(), worldY + worldBody.getWorldOffsetY());
      return light;
    }
  }

  /**
   * Must be called within {@link WorldBody#postBox2dRunnable(Runnable)} or {@link  World#postBox2dRunnable(Runnable)}
   *
   * @deprecated Use {@link #obtain(float, float)} to correctly calculate any world offset
   */
  @Deprecated(forRemoval = false)
  @Override
  public PointLight obtain() {
    synchronized (BOX2D_LOCK) {
      PointLight light;
      do {
        light = super.obtain();
      } while (light == null);
      light.setActive(true);
      return light;

    }
  }

  @Override
  public void free(@NotNull PointLight light) {
    worldBody.postBox2dRunnable(() -> {
      if (!light.isActive()) {
        if (Settings.debug) {
          throw new IllegalStateException("Double light release!");
        } else {
          Main.logger().error("PLP", "Tried to free inactive light");
          return;
        }
      }
      light.setActive(false);
      super.free(light);
    });
  }

  @Override
  protected void discard(@NotNull PointLight light) {
    worldBody.postBox2dRunnable(() -> light.remove(true));
  }

  @Override
  protected void reset(@NotNull PointLight light) {
    worldBody.postBox2dRunnable(() -> {
      light.setPosition(Float.MAX_VALUE, Float.MAX_VALUE);
      light.setStaticLight(true);
      light.setXray(false);
      light.setSoft(true);
      light.setSoftnessLength(World.POINT_LIGHT_SOFTNESS_LENGTH);
      light.setDistance(POINT_LIGHT_DISTANCE);
      light.setColor(Color.WHITE);
      light.setContactFilter(World.LIGHT_FILTER);
    });
  }

  @Override
  public void dispose() {
    clear();
  }
}
