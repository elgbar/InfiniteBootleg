package no.elg.infiniteBootleg.world.blocks;

import box2dLight.Light;
import box2dLight.PointLight;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.util.PointLightPool;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LightBlock extends TickingBlock {

    @Nullable
    private PointLight light;

    public LightBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
        Main.inst().getScheduler().executeSync(this::createLight);
    }

    private void createLight() {
        if (Settings.renderLight && light == null && getChunk().getChunkBody().hasBody()) {
            light = PointLightPool.getPool(getWorld()).obtain(getWorldX() + 0.5f, getWorldY() + 0.5f);
        }
        setShouldTick(true);
    }

    /**
     * @return The light of this block, might be null if {@link Settings#client} is true
     */
    @Nullable
    public Light getLight() {
        createLight();
        if (light == null) {
            setShouldTick(true);
        }
        return light;
    }

    @Override
    public void tick() {
        var gotLight = getLight();
        if (gotLight != null) {
            //Cannot access update light method directly, so this is a hack to force the light mesh to be updated
            gotLight.setStaticLight(true);
        }
    }

    @Override
    public void dispose() {
        if (light != null) {
            PointLightPool.getPool(getWorld()).free(light);
        }
    }
}
