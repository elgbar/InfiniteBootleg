package no.elg.infiniteBootleg.world.blocks;

import no.elg.infiniteBootleg.Ticking;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a block that implements the {@link Ticking} interface.
 * <p>
 * This implementation is set up to automatically call {@link #tick()} and {@link #tickRare()} if {@link #shouldTick()}
 * is {@code true}. To change this behaviour override {@link #shouldTick()}
 * <p>
 * For each chunk has a list of blocks that extends this class and will call {@link #tryTick(boolean)} every tick.
 *
 * @see Chunk
 * @see TntBlock TntBlock (for change of ticking behaviour)
 */
public abstract class TickingBlock extends Block implements Ticking {

    private boolean shouldTick;

    public TickingBlock(@NotNull World world, Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
    }

    /**
     * Update if the update flag is set to true
     *
     * @param rare
     *     If the the rare update should be called instead of the normal update
     */
    public void tryTick(boolean rare) {
        if (shouldTick()) {
            shouldTick = false;
            if (rare) {
                tickRare();
            }
            else {
                tick();
            }
        }
    }

    public boolean shouldTick() {
        return shouldTick;
    }

    public void setShouldTick(boolean shouldTick) {
        this.shouldTick = shouldTick;
    }
}
