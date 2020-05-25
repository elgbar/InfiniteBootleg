package no.elg.infiniteBootleg.world.blocks;

import box2dLight.Light;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * A block that lights up the surrounding area
 */
public class Torch extends StaticLightBlock {

    public Torch(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
        Light light = getLight();
        if (light != null) {
            light.setColor(244 / 255f, 178 / 255f, 153 / 255f, 1);
        }
    }
}
