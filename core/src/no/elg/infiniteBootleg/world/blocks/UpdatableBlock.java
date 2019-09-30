package no.elg.infiniteBootleg.world.blocks;

import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Ticking;
import org.jetbrains.annotations.NotNull;

public abstract class UpdatableBlock extends Block implements Ticking {

    private boolean update;

    public UpdatableBlock(@NotNull World world, Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
    }

    /**
     * Update if the update flag is set to true
     *
     * @param rare
     *     If the the rare update should be called instead of the normal update
     */
    public void tryTick(boolean rare) {
        if (shouldUpdate()) {
            update = false;
            if (rare) {
                tickRare();
            }
            else {
                tick();
            }
        }
    }

    public boolean shouldUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }
}
