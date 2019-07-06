package no.elg.infiniteBootleg.world.blocks;

import box2dLight.PointLight;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.PointLightPool;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public class Torch extends Block {

    private PointLight light;

    public Torch(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
        if (Main.renderGraphic) {

            light = PointLightPool.inst.obtain();
            light.setPosition(getWorldX() + 0.5f, getWorldY() + 0.5f);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        PointLightPool.inst.free(light);
    }
}
