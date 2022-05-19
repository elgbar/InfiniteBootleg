package box2dLight;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import no.elg.infiniteBootleg.light.PointLightPool;

/**
 * @author Elg
 */
public class PublicRayHandler extends RayHandler {

  public PublicRayHandler(World world) {
    super(world);
  }

  public PublicRayHandler(World world, int fboWidth, int fboHeight) {
    super(world, fboWidth, fboHeight);
  }

  /**
   * This array contains all the active lights.
   *
   * <p>NOTE: DO NOT MODIFY THIS LIST
   */
  public Array<Light> getEnabledLights() {
    return lightList;
  }

  /**
   * This array contains all the disabled lights.
   *
   * <p>NOTE: DO NOT MODIFY THIS LIST
   */
  public Array<Light> getDisabledLights() {
    return disabledLights;
  }

  public void renderLightMap() {
    lightMap.render();
  }

  @Override
  public void dispose() {
    super.dispose();
    PointLightPool.clearAllPools();
  }
}
