package no.elg.infiniteBootleg.world.subgrid;

import com.google.common.base.Preconditions;
import java.util.UUID;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class MaterialEntity extends Entity implements Removable {

  public MaterialEntity(@NotNull World world, ProtoWorld.@NotNull Entity protoEntity) {
    super(world, protoEntity);
    if (isDisposed()) {
      return;
    }
    Preconditions.checkArgument(protoEntity.getType() == ProtoWorld.Entity.EntityType.BLOCK);

    Preconditions.checkArgument(protoEntity.hasMaterial());
    final ProtoWorld.Entity.Material entityBlock = protoEntity.getMaterial();

    final Material material = Material.fromOrdinal(entityBlock.getMaterialOrdinal());
    Preconditions.checkArgument(material == getMaterial(), "Different materials");
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
  protected @NotNull ProtoWorld.Entity.EntityType getEntityType() {
    return ProtoWorld.Entity.EntityType.BLOCK;
  }

  @NotNull
  public abstract Material getMaterial();

  @Override
  public ProtoWorld.Entity.@NotNull Builder save() {
    final ProtoWorld.Entity.Builder builder = super.save();
    final ProtoWorld.Entity.Material.Builder materialBuilder =
        ProtoWorld.Entity.Material.newBuilder();

    materialBuilder.setMaterialOrdinal(getMaterial().ordinal());

    builder.setMaterial(materialBuilder.build());
    return builder;
  }
}
