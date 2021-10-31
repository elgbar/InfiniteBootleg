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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FallingBlockEntity extends Entity implements LightTrait {

    private final Material material;
    @Nullable
    private final TextureRegion region;

    @Nullable
    private Block block;

    @Nullable
    private PointLight light;

    private volatile boolean crashed;

    public FallingBlockEntity(@NotNull World world, @NotNull Chunk chunk, @NotNull ProtoWorld.Entity protoEntity) {
        super(world, protoEntity);

        Preconditions.checkArgument(protoEntity.hasMaterial());
        final ProtoWorld.Entity.Material protoEntityMaterial = protoEntity.getMaterial();

        material = Material.fromOrdinal(protoEntityMaterial.getMaterialOrdinal());
        region = Settings.client ? new TextureRegion(material.getTextureRegion()) : null;
    }

    public FallingBlockEntity(@NotNull World world, @NotNull Block block) {
        super(world, block.getWorldX() + 0.5f, block.getWorldY() - 0.5f, false, UUID.randomUUID());
        this.block = block;
        this.material = block.getMaterial();
        region = Settings.client ? new TextureRegion(material.getTextureRegion()) : null;
    }

    @Override
    public boolean canCreateLight() {
        return !isInvalid();
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

                int deltaY = 0;
                while (!world.isAirBlock(newX, newY + deltaY)) {
                    deltaY++;
                }
                world.removeEntity(this);
                world.setBlock(newX, newY + deltaY, material, true);
//              //TODO drop as an item
            });
        }
    }

    @Override
    public void tick() {
        super.tick();
        LightTrait.Companion.tryCreateLight(this);
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

    @Override
    public void dispose() {
        super.dispose();
        if (block != null) {
            block.tryDispose();
            block = null;
        }
        if (light != null) {
            PointLightPool.getPool(getWorld()).free(light);
            light = null;
        }
    }

    @Nullable
    @Override
    public PointLight getLight() {
        return light;
    }

    @NotNull
    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public void setLight(@Nullable PointLight light) {
        if (this.light != null) {
            PointLightPool.getPool(getWorld()).free(this.light);
        }
        this.light = light;
    }

    @Override
    public void customizeLight(@NotNull PointLight light) {
        final Chunk chunk = getChunk();
        if (chunk == null) {
            return;
        }
        if (block == null) {
            block = material.createBlock(getWorld(), chunk, CoordUtil.chunkOffset(getBlockX()), CoordUtil.chunkOffset(getBlockY()));
        }
        if (block instanceof LightTrait lightTrait) {
            //FIXME Somehow block and light are the same!?!?!?!
            lightTrait.customizeLight(light);
            light.setStaticLight(false);
            light.attachToBody(getBody());
        }
        block.tryDispose();
        block = null;
    }
}