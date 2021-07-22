package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.google.common.base.Preconditions;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.items.ItemType;
import no.elg.infiniteBootleg.util.Util;
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

    AIR(null, ItemType.AIR, false, false, false, 0f),
    STONE(1.5f),
    BRICK(2f),
    DIRT(1f),
    GRASS(0.8f),
    TNT(TntBlock.class, 0.5f),
    SAND(SandBlock.class, 1f),
    TORCH(Torch.class, ItemType.BLOCK, false, false, true, 0.1f),
    GLASS(null, ItemType.BLOCK, true, false, true, 0.1f),
    DOOR(Door.class, ItemType.ENTITY, true, true, true, 1f);

    private final Constructor<?> constructor;
    private final boolean solid;
    private final boolean blocksLight;
    private final boolean placable;
    private final float hardness;
    private final TextureRegion texture;
    private final ItemType itemType;

    Material(float hardness) {
        this(null, hardness);
    }

    Material(@Nullable Class<?> impl, float hardness) {
        this(impl, ItemType.BLOCK, true, true, true, hardness);
    }

    /**
     * @param impl
     *     The implementation a block of this material must have
     * @param itemType
     * @param solid
     *     If objects can pass through this material
     * @param blocksLight
     *     If this material will block light
     * @param placable
     *     If a block of this material can be placed by a player
     * @param hardness
     */
    Material(@Nullable Class<?> impl, ItemType itemType, boolean solid, boolean blocksLight, boolean placable,
             float hardness) {
        this.itemType = itemType;
        if (impl != null) {
            if (itemType == ItemType.BLOCK) {
                Preconditions.checkArgument(Util.hasSuperClass(impl, Block.class),
                                            name() + " does not have " + Block.class.getSimpleName() +
                                            " as a super class");
                try {
                    constructor = impl.getDeclaredConstructor(World.class, Chunk.class, int.class, int.class,
                                                              Material.class);

                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("There is no constructor of " + impl.getSimpleName() +
                                                    " with the arguments World, Chunk, int, int, Material");
                }
            }
            else if (itemType == ItemType.ENTITY) {
                Preconditions.checkArgument(Util.hasSuperClass(impl, MaterialEntity.class),
                                            name() + " does not have " + MaterialEntity.class.getSimpleName() +
                                            " as a super class");
                try {
                    constructor = impl.getConstructor(World.class, float.class, float.class);
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("There is no constructor of " + impl.getSimpleName() +
                                                    " with the arguments World, float, float");
                }
            }
            else {
                constructor = null;
            }
        }
        else {
            constructor = null;
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

    public static Material fromByte(byte b) {
        return values()[b];
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
        if (world.getMaterial(worldX, worldY) == AIR) {
            if (isBlock()) {
                return world.setBlock(worldX, worldY, this) != null;
            }
            else if (isEntity()) {
                createEntity(world, worldX, worldY);
                return true;
            }
            throw new IllegalStateException("This material (" + name() + ") is neither a block nor an entity");
        }
        return false;
    }

    public boolean isEntity() {
        return itemType == ItemType.ENTITY;
    }

    @NotNull
    public MaterialEntity createEntity(@NotNull World world, float worldX, float worldY) {
        Preconditions.checkArgument(itemType == ItemType.ENTITY);
        try {
            return (MaterialEntity) constructor.newInstance(world, worldX, worldY);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(String.format(
                "Failed to create entity of the type %s at world %s (%.2f,%.2f)", this, world.toString(), worldX,
                worldY), e);

        }
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
