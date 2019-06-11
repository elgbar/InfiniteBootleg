package no.elg.infiniteBootleg.world.blocks;

import no.elg.infiniteBootleg.world.*;
import org.jetbrains.annotations.NotNull;

public class SandBlock extends UpdatableBlock {

    public SandBlock(@NotNull World world, Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
    }

    @Override
    public void update() {
        Block down = getRelative(Direction.SOUTH);
        if (down.getMaterial() == Material.AIR) {
            getChunk().setBlock(getChunkLoc().x, getChunkLoc().y, null);
            getWorld().setBlock(down.getWorldLoc(), getMaterial());
        }
    }
}
