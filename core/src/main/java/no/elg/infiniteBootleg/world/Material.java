package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectSet;
import com.google.common.base.Preconditions;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.items.ItemType;
import no.elg.infiniteBootleg.protobuf.Proto;
import no.elg.infiniteBootleg.util.CoordUtil;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.blocks.EntityBlock;
import no.elg.infiniteBootleg.world.blocks.SandBlock;
import no.elg.infiniteBootleg.world.blocks.TntBlock;
import no.elg.infiniteBootleg.world.blocks.Torch;
import no.elg.infiniteBootleg.world.subgrid.MaterialEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Door;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public enum Material {

    AIR(null, ItemType.AIR, 0f, false, false, false),
    STONE(1.5f),
    BRICK(2f),
    DIRT(1f),
    GRASS(0.8f),
    TNT(TntBlock.class, ItemType.BLOCK, 0.5f, true, false, true),
    SAND(SandBlock.class, 1f),
    TORCH(Torch.class, ItemType.BLOCK, 0.1f, false, false, true),
    GLASS(null, ItemType.BLOCK, 0.1f, true, false, true),
    DOOR(Door.class, ItemType.ENTITY, 1f, false, false, true),
    ;
    @Nullable
    private final Constructor<?> constructor;
    @Nullable
    private final Constructor<?> constructorProtoBuf;
    private final boolean solid;
    private final boolean blocksLight;
    private final boolean placable;
    private final float hardness;
    @Nullable
    private final TextureRegion texture;
    @NotNull
    private final ItemType itemType;

    private static final Material[] VALUES = values();

    Material(float hardness) {
        this(null, hardness);
    }

    Material(@Nullable Class<?> impl, float hardness) {
        this(impl, ItemType.BLOCK, hardness, true, true, true);
    }

    /**
     * @param impl
     *     The implementation a block of this material must have
     * @param itemType
     * @param hardness
     * @param solid
     *     If objects can pass through this material
     * @param blocksLight
     *     If this material will block light
     * @param placable
     *     If a block of this material can be placed by a player
     */
    Material(@Nullable Class<?> impl, @NotNull ItemType itemType, float hardness, boolean solid, boolean blocksLight, boolean placable) {
        this.itemType = itemType;
        if (impl != null) {
            if (itemType == ItemType.BLOCK) {
                Preconditions.checkArgument(Util.hasSuperClass(impl, Block.class),
                                            name() + " does not have " + Block.class.getSimpleName() + " as a super class");
                try {
                    constructor = impl.getDeclaredConstructor(World.class, Chunk.class, int.class, int.class, Material.class);
                    constructorProtoBuf = null;
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(
                        "There is no constructor of " + impl.getSimpleName() + " with the arguments World, Chunk, int, int, Material");
                }
            }
            else if (itemType == ItemType.ENTITY) {
                Preconditions.checkArgument(Util.hasSuperClass(impl, MaterialEntity.class),
                                            name() + " does not have " + MaterialEntity.class.getSimpleName() + " as a super class");
                try {
                    constructor = impl.getConstructor(World.class, float.class, float.class);
                    constructorProtoBuf = impl.getConstructor(World.class, Proto.Entity.class);
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("There is no constructor of " + impl.getSimpleName() + " with the arguments World, float, float");
                }
            }
            else {
                constructor = null;
                constructorProtoBuf = null;
            }
        }
        else {
            constructor = null;
            constructorProtoBuf = null;
        }
        this.solid = solid;
        this.blocksLight = blocksLight;
        this.placable = placable;
        this.hardness = hardness;
        if (Settings.renderGraphic && itemType != ItemType.AIR) {
            texture = Main.inst().getBlockAtlas().findRegion(name().toLowerCase());
            if (texture == null) {
                throw new NullPointerException("Failed to find a texture for " + name());
            }
        }
        else {
            texture = null;
        }
    }

    public static @NotNull Material fromOrdinal(int b) {
        return VALUES[b];
    }

    /**
     * @param world
     *     World this block this exists in
     * @param chunk
     * @param localX
     *     Relative x in the chunk
     * @param localY
     *     Relative y in the chunk
     *
     * @return A block of this type
     */
    @NotNull
    public Block createBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY) {
        Preconditions.checkArgument(isBlock());
        if (constructor == null) {
            return new Block(world, chunk, localX, localY, this);
        }
        try {
            return (Block) constructor.newInstance(world, chunk, localX, localY, this);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isBlock() {
        return itemType == ItemType.BLOCK || itemType == ItemType.AIR;
    }

    public boolean create(@NotNull World world, int worldX, int worldY) {
        return create(world, worldX, worldY, false);
    }

    public boolean create(@NotNull World world, int worldX, int worldY, boolean prioritize) {
        if (world.getMaterial(worldX, worldY) == AIR) {
            if (isBlock()) {
                return world.setBlock(worldX, worldY, this, prioritize) != null;
            }
            else if (isEntity()) {
                return createEntity(world, worldX, worldY) != null;
            }
            throw new IllegalStateException("This material (" + name() + ") is neither a block nor an entity");
        }
        return false;
    }

    public boolean isEntity() {
        return itemType == ItemType.ENTITY;
    }

    @Nullable
    public MaterialEntity createEntity(@NotNull World world, @NotNull Proto.Entity protoEntity, @NotNull Chunk chunk) {
        Preconditions.checkArgument(itemType == ItemType.ENTITY);
        Preconditions.checkNotNull(constructorProtoBuf, "Constructor of entity cannot be null");
        MaterialEntity entity;
        try {
            entity = (MaterialEntity) constructorProtoBuf.newInstance(world, protoEntity);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        return commonEntity(world, entity, chunk);
    }

    @Nullable
    public MaterialEntity createEntity(@NotNull World world, float worldX, float worldY) {
        Preconditions.checkArgument(itemType == ItemType.ENTITY);
        Preconditions.checkNotNull(constructor, "Constructor of entity cannot be null");
        MaterialEntity entity;
        try {
            entity = (MaterialEntity) constructor.newInstance(world, worldX, worldY);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }

        return commonEntity(world, entity, null);
    }

    @Nullable
    private MaterialEntity commonEntity(@NotNull World world, @NotNull MaterialEntity entity, @Nullable Chunk chunk) {
        if (entity.isInvalid()) {
            return null;
        }
        world.addEntity(entity, false);
        final ObjectSet<Location> locations = entity.touchingLocations();
        for (Location location : locations) {
            Chunk locChunk;
            if (chunk != null && CoordUtil.worldToChunk(location.x) == chunk.getChunkX() && CoordUtil.worldToChunk(location.y) == chunk.getChunkY()) {
                locChunk = chunk;
            }
            else {
                locChunk = world.getChunkFromWorld(location);
            }

            if (locChunk == null) {
                Main.logger().error("MATERIAL", "Failed get chunk for entity block");
                continue;
            }
            final int localX = CoordUtil.chunkOffset(location.x);
            final int localY = CoordUtil.chunkOffset(location.y);
            var block = new EntityBlock(world, locChunk, localX, localY, this, entity);
            locChunk.setBlock(localX, localY, block, false);
        }
        return entity;
    }

    @Nullable
    public TextureRegion getTextureRegion() {
        return texture;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean blocksLight() {
        return blocksLight;
    }

    public boolean isPlacable() {
        return placable;
    }

    public float getHardness() {
        return hardness;
    }
}
