package no.elg.infiniteBootleg.world.subgrid;

import com.google.common.base.Preconditions;
import java.util.UUID;
import no.elg.infiniteBootleg.protobuf.Proto;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class MaterialEntity extends Entity implements Removable {

    public MaterialEntity(@NotNull World world, Proto.@NotNull Entity protoEntity) {
        super(world, protoEntity);
        Preconditions.checkArgument(protoEntity.getType() == Proto.Entity.EntityType.BLOCK);

        Preconditions.checkArgument(protoEntity.hasBlock());
        final Proto.Entity.BlockEntity entityBlock = protoEntity.getBlock();

        final Material material = Material.fromOrdinal(entityBlock.getMaterialOrdinal());
        Preconditions.checkArgument(material == getMaterial(), "Different materials");

        final Proto.Vector2i blockPosition = entityBlock.getPosition();
        teleport(blockPosition.getX(), blockPosition.getY(), false);

    }

    public MaterialEntity(@NotNull World world, float worldX, float worldY) {
        super(world, worldX, worldY, UUID.randomUUID());
    }

    @Override
    public String toString() {
        return "MaterialEntity{" + "material='" + getMaterial() + '\'' + "} " + super.toString();
    }

    @Override
    public void onRemove() {
        for (Block block : touchingBlocks()) {
            block.destroy(true);
        }
    }

    @Override
    protected @NotNull Proto.Entity.EntityType getEntityType() {
        return Proto.Entity.EntityType.BLOCK;
    }

    @NotNull
    public abstract Material getMaterial();


    @Override
    public Proto.Entity.Builder save() {
        final Proto.Entity.Builder builder = super.save();
        final Proto.Entity.BlockEntity.Builder blockBuilder = Proto.Entity.BlockEntity.newBuilder();
        blockBuilder.setMaterialOrdinal(getMaterial().ordinal());
        blockBuilder.setPosition(Proto.Vector2i.newBuilder().setX(getBlockX()).setY(getBlockY()).build());
        builder.setBlock(blockBuilder.build());
        return builder;
    }

}
