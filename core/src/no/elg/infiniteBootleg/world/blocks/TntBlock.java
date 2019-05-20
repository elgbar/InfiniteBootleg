package no.elg.infiniteBootleg.world.blocks;

import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Updatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class TntBlock extends Block implements Updatable {

    public TntBlock(int x, int y, @Nullable World world, @NotNull Material material) {
        super(x, y, world, material);
    }

    @Override
    public void update() {

    }
}
