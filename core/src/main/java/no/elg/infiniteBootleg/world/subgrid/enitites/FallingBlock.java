package no.elg.infiniteBootleg.world.subgrid.enitites;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.Material.AIR;
import static no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction.PUSH_UP;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.google.common.base.Preconditions;
import java.util.UUID;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction;
import no.elg.infiniteBootleg.world.subgrid.contact.ContactType;
import org.jetbrains.annotations.NotNull;

public class FallingBlock extends Entity {

    private final Material material;
    private final TextureRegion region;

    private volatile boolean crashed;

    public FallingBlock(@NotNull World world, ProtoWorld.@NotNull Entity protoEntity) {
        super(world, protoEntity);

        Preconditions.checkArgument(protoEntity.hasMaterial());
        final ProtoWorld.Entity.Material protoEntityMaterial = protoEntity.getMaterial();

        material = Material.fromOrdinal(protoEntityMaterial.getMaterialOrdinal());
        region = new TextureRegion(material.getTextureRegion());
    }

    public FallingBlock(@NotNull World world, float worldX, float worldY, @NotNull Material material) {
        super(world, worldX + 0.5f, worldY + 0.5f, false, UUID.randomUUID());
        this.material = material;
        region = new TextureRegion(material.getTextureRegion());
    }

    @Override
    protected void createFixture(@NotNull Body body) {
        PolygonShape box = new PolygonShape();
        box.setAsBox(getHalfBox2dWidth(), getHalfBox2dHeight());
        Fixture fix = body.createFixture(box, 1.0f);
        fix.setFilterData(World.FALLING_BLOCK_ENTITY_FILTER);
        box.dispose();
    }

    @Override
    public synchronized void contact(@NotNull ContactType type, @NotNull Contact contact) {
        if (isInvalid()) {
            return;
        }
        if (!crashed && type == ContactType.BEGIN_CONTACT) {
            crashed = true;

            Main.inst().getScheduler().executeSync(() -> {
                World world = getWorld();
                int newX = getBlockX();
                int newY = getBlockY();

                var deltaY = 0;
                while (true) {
                    Block block = world.getBlock(newX, newY + deltaY, true);
                    if (block == null || block.getMaterial() == AIR) {
                        break;
                    }
                    deltaY++;
                }
                world.removeEntity(this);
                world.setBlock(newX, newY + deltaY, material, true);
//                else{
//                    //TODO drop as an item
//                }
            });
        }
    }

    @Override
    public boolean isOnGround() {
        return false;
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
        //Unload this entity if it entered an unloaded chunk
        int chunkX = CoordUtil.worldToChunk(getBlockX());
        int chunkY = CoordUtil.worldToChunk(getBlockY());

        //remove entity if it no longer falling and have not become a true block for some reason
        if (!getWorld().isChunkLoaded(chunkX, chunkY) || getVelocity().isZero()) {
            Main.inst().getScheduler().executeAsync(() -> getWorld().removeEntity(this));
        }
    }

    @Override
    protected @NotNull ProtoWorld.Entity.EntityType getEntityType() {
        return ProtoWorld.Entity.EntityType.FALLING_BLOCK;
    }

    @Override
    public ProtoWorld.Entity.Builder save() {
        final ProtoWorld.Entity.Builder builder = super.save();
        final ProtoWorld.Entity.Material.Builder materialBuilder = ProtoWorld.Entity.Material.newBuilder();

        materialBuilder.setMaterialOrdinal(material.ordinal());

        builder.setMaterial(materialBuilder.build());
        return builder;
    }
}
