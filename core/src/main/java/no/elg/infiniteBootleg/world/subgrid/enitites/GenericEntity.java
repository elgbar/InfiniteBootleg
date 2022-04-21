package no.elg.infiniteBootleg.world.subgrid.enitites;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.google.common.base.Preconditions;
import java.util.UUID;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.Removable;
import org.jetbrains.annotations.NotNull;

public class GenericEntity extends Entity implements Removable {

  private int width;
  private int height;

  public GenericEntity(@NotNull World world, ProtoWorld.@NotNull Entity protoEntity) {
    super(world, protoEntity);
    if (isDisposed()) {
      return;
    }

    Preconditions.checkArgument(protoEntity.hasGeneric());
    var protoGeneric = protoEntity.getGeneric();

    final ProtoWorld.Vector2i protoSize = protoGeneric.getSize();
    width = protoSize.getX();
    height = protoSize.getY();
  }

  public GenericEntity(@NotNull World world, float worldX, float worldY) {
    this(world, worldX, worldY, 1, 1);
  }

  public GenericEntity(@NotNull World world, float worldX, float worldY, int width, int height) {
    super(world, worldX, worldY, true, UUID.randomUUID());

    this.width = width * Block.BLOCK_SIZE;
    this.height = height * Block.BLOCK_SIZE;
  }

  @Override
  @NotNull
  protected BodyDef createBodyDef(float worldX, float worldY) {
    BodyDef def = super.createBodyDef(worldX, worldY);
    def.type = BodyDef.BodyType.StaticBody;
    return def;
  }

  @Override
  public ProtoWorld.Entity.@NotNull Builder save() {
    final ProtoWorld.Entity.Builder builder = super.save();
    final ProtoWorld.Entity.Generic.Builder genericBuilder = ProtoWorld.Entity.Generic.newBuilder();

    genericBuilder.setSize(ProtoWorld.Vector2i.newBuilder().setX(width).setY(height).build());

    builder.setGeneric(genericBuilder.build());
    return builder;
  }

  @Override
  public TextureRegion getTextureRegion() {
    return null;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  protected @NotNull ProtoWorld.Entity.EntityType getEntityType() {
    return ProtoWorld.Entity.EntityType.GENERIC_ENTITY;
  }
}
