package no.elg.infiniteBootleg.world.blocks;

import com.badlogic.gdx.Gdx;
import no.elg.infiniteBootleg.world.*;
import no.elg.infiniteBootleg.world.subgrid.FallingBlock;
import org.jetbrains.annotations.NotNull;

public class SandBlock extends UpdatableBlock {

    public SandBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
    }

    @Override
    public void update() {
        if (getWorld().isAir(Location.relative(getWorldX(), getWorldY(), Direction.SOUTH))) {
            Gdx.app.postRunnable(() -> {
                getChunk().setBlock(getLocalX(), getLocalY(), null, true);
                new FallingBlock(getWorld(), getWorldX(), getWorldY(), Material.SAND);
            });
        }
    }
}
