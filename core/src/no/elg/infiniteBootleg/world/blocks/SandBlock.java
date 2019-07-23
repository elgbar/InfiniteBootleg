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
        Gdx.app.postRunnable(() -> {
            Location south = Location.relative(getWorldX(), getWorldY(), Direction.SOUTH);
            if (getWorld().isAir(south) && getWorld().getEntity(south.x, south.y) == null) {
                getChunk().setBlock(getLocalX(), getLocalY(), (Block) null, true);
                new FallingBlock(getWorld(), getWorldX() + 0.5f, getWorldY() + 0.5f, Material.SAND);
            }
        });

    }
}
