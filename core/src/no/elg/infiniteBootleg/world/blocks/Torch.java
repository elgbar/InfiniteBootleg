package no.elg.infiniteBootleg.world.blocks;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Pool;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public class Torch extends Block {

    public static StaticPointLightPool lightPool = new StaticPointLightPool();

    private PointLight light;

    public Torch(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
        light = lightPool.obtain();
        light.setPosition(getWorldLoc().x + 0.5f, getWorldLoc().y + 0.5f);
    }

    @Override
    public void dispose() {
        super.dispose();
        lightPool.free(light);
    }

    private static class StaticPointLightPool extends Pool<PointLight> {

        private final RayHandler rayHandler;

        public static final int POINT_LIGHT_RAYS = 32;
        public static final int POINT_LIGHT_DISTANCE = 5;

        public StaticPointLightPool() {
            rayHandler = Main.inst().getWorld().getRender().getRayHandler();
        }

        @Override
        public PointLight obtain() {
            PointLight light = super.obtain();
            light.setActive(true);
            return light;
        }

        @Override
        protected PointLight newObject() {
            PointLight light = new PointLight(rayHandler, POINT_LIGHT_RAYS, Color.BLACK, POINT_LIGHT_DISTANCE, 0, 0);
            light.setStaticLight(true);
            return light;
        }

        @Override
        protected void reset(PointLight object) {
            object.setActive(false);
        }
    }
}
