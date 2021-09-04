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
        createLight();
    }

    private void createLight() {
        if (Settings.renderLight && light == null) {
            light = PointLightPool.getPool(getWorld()).obtain(getWorldX() + 0.5f, getWorldY() + 0.5f);
        }
    }

    /**
     * @return The light of this block, might be null if {@link Settings#renderGraphic} is true
     */
    @Nullable
    public Light getLight() {
        createLight();
        return light;
    }

    @Override
    public void dispose() {
        if (light != null) {
            PointLightPool.getPool(getWorld()).free(light);
        }
    }
}
