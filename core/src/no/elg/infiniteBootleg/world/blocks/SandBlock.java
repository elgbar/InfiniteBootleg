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
        Block below = getRelative(Direction.SOUTH);
        if (below.getMaterial() == Material.AIR) {
            Gdx.app.postRunnable(() -> {
                getChunk().setBlock(getChunkLoc().x, getChunkLoc().y, null);
                getWorld().getEntities().add(new FallingBlock(getWorld(), getWorldLoc().x, getWorldLoc().y, Material.SAND));
            });
        }
    }
}
