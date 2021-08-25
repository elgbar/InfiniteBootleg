package no.elg.infiniteBootleg.world.blocks;

import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.MaterialEntity;
import no.elg.infiniteBootleg.world.subgrid.Removable;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class EntityBlock extends Block implements Removable {

    @NotNull
    private final MaterialEntity entity;

    public EntityBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY, @NotNull Material material, @NotNull MaterialEntity entity) {
        super(world, chunk, localX, localY, material);
        this.entity = entity;
    }

    @NotNull
    public MaterialEntity getEntity() {
        return entity;
    }
}
