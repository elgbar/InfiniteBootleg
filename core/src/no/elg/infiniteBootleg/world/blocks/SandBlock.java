package no.elg.infiniteBootleg.world.blocks;

import com.badlogic.gdx.Gdx;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Direction;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.FallingBlock;
import org.jetbrains.annotations.NotNull;

public class SandBlock extends UpdatableBlock {

    public SandBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
    }

    @Override
    public void update() {
        if (getWorld().isAir(getWorldLoc().relative(Direction.SOUTH))) {
            Gdx.app.postRunnable(() -> {
                getChunk().setBlock(getLocalChunkLoc().x, getLocalChunkLoc().y, null, true);
                new FallingBlock(getWorld(), getWorldLoc().x, getWorldLoc().y, Material.SAND);
            });
        }
    }
}
