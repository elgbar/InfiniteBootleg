package no.elg.infiniteBootleg.world.blocks;

import com.badlogic.gdx.Gdx;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.*;
import no.elg.infiniteBootleg.world.subgrid.enitites.FallingBlock;
import org.jetbrains.annotations.NotNull;

/**
 * A blocks that falls when ticked
 */
public class SandBlock extends TickingBlock {

    public SandBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material) {
        super(world, chunk, localX, localY, material);
    }

    @Override
    public void tick() {
        Location south = Location.relative(getWorldX(), getWorldY(), Direction.SOUTH);
        if (getWorld().isAir(south)) {
            Gdx.app.postRunnable(() -> {
                if (getChunk().isLoaded()) {
                    destroy();
                    Main.inst().getScheduler().scheduleSync(() -> {
                        new FallingBlock(getWorld(), getWorldX(), getWorldY(), Material.SAND);
                    }, 10L);
                }
            });
        }
    }
}
