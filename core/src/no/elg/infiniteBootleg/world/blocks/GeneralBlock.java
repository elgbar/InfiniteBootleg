package no.elg.infiniteBootleg.world.blocks;

import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A class for the materials that have no special effects
 *
 * @author Elg
 */
public class GeneralBlock extends Block {

    private final Material material;

    public GeneralBlock(int x, int y, @Nullable World world, Material material) {
        super(x, y, world);
        this.material = material;
    }

    @Override
    public @NotNull Material getMaterial() {
        return material;
    }
}
