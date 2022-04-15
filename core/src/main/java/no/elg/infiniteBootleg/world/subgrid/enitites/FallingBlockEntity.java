package no.elg.infiniteBootleg.world.subgrid.enitites;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction.PUSH_UP;

import box2dLight.PointLight;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.google.common.base.Preconditions;
import java.util.UUID;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.Packets;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.PointLightPool;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.blocks.traits.LightTrait;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction;
import no.elg.infiniteBootleg.world.subgrid.contact.ContactType;
import no.elg.infiniteBootleg.world.ticker.WorldLightTicker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FallingBlockEntity extends Entity implements LightTrait {

  private Material material;
  @Nullable private TextureRegion region;

  @NotNull private Block block;

  private volatile boolean crashed;

  public FallingBlockEntity(
      @NotNull World world, @NotNull Chunk chunk, @NotNull ProtoWorld.Entity protoEntity) {
    super(world, protoEntity);
    if (isInvalid()) {
      return;
    }
    Preconditions.checkArgument(protoEntity.hasMaterial());
    final ProtoWorld.Entity.Material protoEntityMaterial = protoEntity.getMaterial();

    material = Material.fromOrdinal(protoEntityMaterial.getMaterialOrdinal());
    region = Settings.client ? material.getTextureRegion() : null;
    final ProtoWorld.Vector2f position = protoEntity.getPosition();
    block = material.createBlock(world, chunk, (int) position.getX(), (int) position.getY());
  }

  public FallingBlockEntity(@NotNull World world, @NotNull Block block) {
    super(
        world, block.getWorldX() + 0.5f, block.getWorldY() + 0.5f, false, UUID.randomUUID(), false);
    if (isInvalid()) {
      return;
    }
    this.block = block;
    this.material = block.getMaterial();
    region = Settings.client ? material.getTextureRegion() : null;
  }

  @Override
  public boolean canCreateLight() {
    return block instanceof LightTrait && !isInvalid();
  }

  @Override
  protected void createFixture(@NotNull Body body) {
    PolygonShape box = new PolygonShape();
    box.setAsBox(getHalfBox2dWidth() - 0.1f, getHalfBox2dHeight() - 0.1f);
    Fixture fix = body.createFixture(box, 1.0f);
    fix.setFilterData(World.FALLING_BLOCK_ENTITY_FILTER);
    Main.inst()
        .getScheduler()
        .scheduleAsync(
            100L,
            () -> {
              if (material.blocksLight()) {
                fix.setFilterData(World.FALLING_BLOCK_BLOCKS_LIGHT_ENTITY_FILTER);
              }
            });
    fix.setFriction(0f);
    box.dispose();
  }

  @Override
  public synchronized void contact(@NotNull ContactType type, @NotNull Contact contact) {
    if (isInvalid()) {
      return;
    }
    if (!crashed && type == ContactType.BEGIN_CONTACT) {
      crashed = true;

      freeze(true);
      if (Main.isAuthoritative()) {
        Main.inst()
            .getScheduler()
            .executeAsync(
                () -> {
                  var world = getWorld();
                  int newX = getBlockX();
                  int newY = getBlockY();

                  int deltaY = 0;
                  while (!world.isAirBlock(newX, newY + deltaY)) {
                    deltaY++;
                  }
                  world.removeEntity(this, Packets.DespawnEntity.DespawnReason.NATURAL);
                  world.setBlock(newX, newY + deltaY, material, true);
                  //              //TODO drop as an item
                });
      }
    }
  }

  @Override
  public void tick() {
    if (crashed) {
      return;
    }
    super.tick();
    if (block instanceof LightTrait) {
      LightTrait.Companion.createLight(this);
    }
    if (material.blocksLight()) {
      WorldLightTicker.updateDirectionalLights();
    }
  }

  @Override
  public boolean isOnGround() {
    return crashed;
  }

  @Override
  public TextureRegion getTextureRegion() {
    return region;
  }

  @Override
  public int getWidth() {
    return BLOCK_SIZE - 1;
  }

  @Override
  public int getHeight() {
    return BLOCK_SIZE - 1;
  }

  @Override
  public InvalidSpawnAction invalidSpawnLocationAction() {
    return PUSH_UP;
  }

  @Override
  public void tickRare() {
    // Unload this entity if it entered an unloaded chunk
    int chunkX = CoordUtil.worldToChunk(getBlockX());
    int chunkY = CoordUtil.worldToChunk(getBlockY());

    if (!getWorld().isChunkLoaded(chunkX, chunkY) || getVelocity().isZero()) {
      if (Main.isAuthoritative()) {
        // remove entity if it no longer falling and have not become a true block for some reason
        Main.inst().getScheduler().executeAsync(() -> getWorld().removeEntity(this));
      } else {
        freeze(true);
      }
    }
  }

  @Override
  protected @NotNull ProtoWorld.Entity.EntityType getEntityType() {
    return ProtoWorld.Entity.EntityType.FALLING_BLOCK;
  }

  @Override
  public ProtoWorld.Entity.@NotNull Builder save() {
    final ProtoWorld.Entity.Builder builder = super.save();
    final ProtoWorld.Entity.Material.Builder materialBuilder =
        ProtoWorld.Entity.Material.newBuilder();

    materialBuilder.setMaterialOrdinal(material.ordinal());

    builder.setMaterial(materialBuilder.build());
    return builder;
  }

  @Override
  public void dispose() {
    super.dispose();
    if (!block.isDisposed()) {
      block.dispose();
    }
  }

  @Nullable
  @Override
  public PointLight getLight() {
    if (block instanceof LightTrait lightTrait) {
      return lightTrait.getLight();
    }
    return null;
  }

  @NotNull
  @Override
  public Block getBlock() {
    return block;
  }

  @Override
  public void setLight(@Nullable PointLight light) {
    if (block instanceof LightTrait lightTrait) {
      lightTrait.setLight(light);
    }
    if (light != null) {
      PointLightPool pool = PointLightPool.getPool(block.getWorld());
      if (pool != null) {
        pool.free(light);
      }
    }
  }

  @Override
  public void customizeLight(@NotNull PointLight light) {
    Preconditions.checkState(block instanceof LightTrait);
    //    final Chunk chunk = getChunk();
    //    if (chunk == null) {
    //      return;
    //    }
    //    if (block instanceof LightTrait lightTrait) {
    ((LightTrait) block).customizeLight(light);
    light.setStaticLight(false);
    light.attachToBody(getBody());
    //    }
  }
}
