package no.elg.infiniteBootleg.world.blocks;

import com.badlogic.gdx.Gdx;
import no.elg.infiniteBootleg.world.*;
import no.elg.infiniteBootleg.world.subgrid.enitites.FallingBlock;
import org.jetbrains.annotations.NotNull;

public class SandBlock extends UpdatableBlock {

    public SandBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
    }

    @Override
    public void update() {
        Location south = Location.relative(getWorldX(), getWorldY(), Direction.SOUTH);
        if (getWorld().isAir(south)) {
            Gdx.app.postRunnable(() -> {
                if (getChunk().isLoaded()) {
                    getChunk().setBlock(getLocalX(), getLocalY(), (Block) null, true);
                    new FallingBlock(getWorld(), getWorldX(), getWorldY(), Material.SAND);
                }
            });
        }
    }
}
