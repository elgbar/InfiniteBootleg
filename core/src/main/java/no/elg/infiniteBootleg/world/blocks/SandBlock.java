package no.elg.infiniteBootleg.world.blocks;

import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Direction;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.enitites.FallingBlock;
import org.jetbrains.annotations.NotNull;

/**
 * A blocks that falls when ticked
 */
public class SandBlock extends TickingBlock {

    public SandBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
    }

    private boolean falling;

    @Override
    public synchronized void tick() {
        if (falling) {
            return;
        }
        Location south = Location.relative(getWorldX(), getWorldY(), Direction.SOUTH);
        if (getWorld().isAirBlock(south)) {
            falling = true;

            Main.inst().getScheduler().executeAsync(() -> {
                //Do not update world straight away as if there are sand blocks above this it will begin to fall on the same tick
                destroy();
                new FallingBlock(getWorld(), getWorldX(), getWorldY() - 1f, Material.SAND);

                Block relative = getRelative(Direction.NORTH);
                if (relative instanceof TickingBlock tickingBlock) {
                    //Wait a bit to let the falling block gain some momentum
                    tickingBlock.delayedShouldTick(getWorld().getWorldTicker().getTPS() / 10L);
                }
            });
        }
    }
}
