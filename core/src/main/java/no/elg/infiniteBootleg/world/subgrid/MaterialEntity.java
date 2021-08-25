package no.elg.infiniteBootleg.world.subgrid;

import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class MaterialEntity extends Entity implements Removable {


    public MaterialEntity(@NotNull World world, float worldX, float worldY) {
        super(world, worldX, worldY);
    }

    @Override
    public String toString() {
        return "MaterialEntity{" + "material='" + getMaterial() + '\'' + "} " + super.toString();
    }

    @Override
    public void onRemove() {
        for (Block block : touchingBlocks()) {
            block.destroy();
        }
    }

    public abstract Material getMaterial();

}
