package no.elg.infiniteBootleg.world.blocks;

import box2dLight.Light;
import box2dLight.PointLight;
import com.badlogic.gdx.graphics.Color;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public class Torch extends Block {

    private Light light;

    public Torch(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
        light =
            new PointLight(world.getRender().getRayHandler(), 8, new Color(0f, 0f, 0f, 1), 15, getWorldLoc().x, getWorldLoc().y);
        light.setXray(true);
    }

    @Override
    public void dispose() {
        super.dispose();
        //FIXME use a cache for lights
        light.dispose();
    }
}
