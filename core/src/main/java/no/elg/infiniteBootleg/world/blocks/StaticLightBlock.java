package no.elg.infiniteBootleg.world.blocks;

import box2dLight.Light;
import box2dLight.PointLight;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.util.PointLightPool;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class StaticLightBlock extends Block {

    @Nullable
    private PointLight light;

    public StaticLightBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
        if (Settings.renderGraphic) {
            light = PointLightPool.inst.obtain();
            light.setPosition(getWorldX() + 0.5f, getWorldY() + 0.5f);
        }
    }

    /**
     * Update the light to make sure it collides with the current surrounding blocks
     */
    public void updateLight() {
        if (light == null) {
            return;
        }
        light.setPosition(light.getPosition());
    }

    /**
     * @return The light of this block, might be null if {@link Settings#renderGraphic} is true
     */
    @Nullable
    public Light getLight() {
        return light;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (light != null) {
            PointLightPool.inst.free(light);
        }
    }
}
