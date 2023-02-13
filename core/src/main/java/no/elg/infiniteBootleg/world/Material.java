package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.google.common.base.Preconditions;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import kotlin.NotImplementedError;
import no.elg.infiniteBootleg.KAssets;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.items.ItemType;
import no.elg.infiniteBootleg.world.blocks.FallingBlock;
import no.elg.infiniteBootleg.world.blocks.TickingBlock;
import no.elg.infiniteBootleg.world.blocks.TntBlock;
import no.elg.infiniteBootleg.world.blocks.Torch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public enum Material {
  AIR(null, ItemType.AIR, 0f, false, false, false, false, true),
  STONE(1.5f),
  BRICK(2f),
  DIRT(1f),
  GRASS(0.8f),
  TNT(TntBlock.class, ItemType.BLOCK, 0.5f, true, true, true, true, false),
  SAND(FallingBlock.class, 1f),
  TORCH(Torch.class, ItemType.BLOCK, 0.1f, false, false, true, true, true),
  GLASS(null, ItemType.BLOCK, 0.1f, true, false, true, false, true),
  DOOR(null, ItemType.BLOCK, 1f, false, true, true, false, true),
  ;
  @Nullable private final Constructor<?> constructor;
  @Nullable private final Constructor<?> constructorProtoBuf;
  private final boolean solid;
  private final boolean blocksLight;
  private final boolean placable;
  private final float hardness;
  @Nullable private final TextureRegion texture;
  @NotNull private final ItemType itemType;
  private final boolean luminescent;
  private final boolean transparent;

  private static final Material[] VALUES = values();

  Material(float hardness) {
    this(null, hardness);
  }

  Material(@Nullable Class<?> impl, float hardness) {
    this(impl, ItemType.BLOCK, hardness, true, true, true, false, false);
  }

  /**
   * @param impl The implementation a block of this material must have
   * @param itemType
   * @param hardness
   * @param solid If objects can pass through this material
   * @param blocksLight If this material will block light
   * @param placable If a block of this material can be placed by a player
   * @param transparent
   */
  Material(
      @Nullable Class<?> impl,
      @NotNull ItemType itemType,
      float hardness,
      boolean solid,
      boolean blocksLight,
      boolean placable,
      boolean luminescent,
      boolean transparent) {
    this.itemType = itemType;
    this.luminescent = luminescent;
    this.transparent = transparent;
    if (impl != null) {
      if (itemType == ItemType.BLOCK) {
        Preconditions.checkArgument(
            Block.class.isAssignableFrom(impl),
            name() + " does not have " + Block.class.getSimpleName() + " as a super type");
        try {
          constructor =
              impl.getDeclaredConstructor(
                  World.class, Chunk.class, int.class, int.class, Material.class);
          constructorProtoBuf = null;
        } catch (NoSuchMethodException e) {
          throw new IllegalStateException(
              "There is no constructor of "
                  + impl.getSimpleName()
                  + " with the arguments World, Chunk, int, int, Material");
        }
      } else {
        constructor = null;
        constructorProtoBuf = null;
      }
    } else {
      constructor = null;
      constructorProtoBuf = null;
    }
    this.solid = solid;
    this.blocksLight = blocksLight;
    this.placable = placable;
    this.hardness = hardness;
    if (Settings.client && itemType != ItemType.AIR) {
      texture = KAssets.INSTANCE.getBlockAtlas().findRegion(name().toLowerCase());
      if (texture == null) {
        throw new NullPointerException("Failed to find a texture for " + name());
      }
    } else {
      texture = null;
    }
  }

  public static @NotNull Material fromOrdinal(int b) {
    return VALUES[b];
  }

  /**
   * @param world World this block this exists in
   * @param chunk
   * @param localX Relative x in the chunk
   * @param localY Relative y in the chunk
   * @return A block of this type
   */
  @NotNull
  public Block createBlock(@NotNull World world, @NotNull Chunk chunk, int localX, int localY) {
    Preconditions.checkArgument(isBlock());
    if (constructor == null) {
      return new BlockImpl(world, chunk, localX, localY, this);
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
    Material currentMaterial = world.getMaterial(worldX, worldY);
    if (currentMaterial != this && currentMaterial == AIR) {
      if (isBlock()) {

        Block block = world.setBlock(worldX, worldY, this, true, true);
        if (block instanceof TickingBlock tickingBlock) {
          tickingBlock.delayedShouldTick(1L);
        }
        return block != null;
      } else if (isEntity()) {
        throw new NotImplementedError("TODO :)");
      }
      throw new IllegalStateException(
          "This material (" + name() + ") is neither a block nor an entity");
    }
    return false;
  }

  public boolean isEntity() {
    return itemType == ItemType.ENTITY;
  }

  @Nullable
  public TextureRegion getTextureRegion() {
    return texture;
  }

  public boolean isSolid() {
    return solid;
  }

  public boolean isBlocksLight() {
    return blocksLight;
  }

  public boolean isPlacable() {
    return placable;
  }

  /**
   * @return If the texture of the material has any transparency
   */
  public boolean isTransparent() {
    return transparent;
  }

  public float getHardness() {
    return hardness;
  }

  /**
   * @return If this material emits light
   */
  public boolean isLuminescent() {
    return luminescent;
  }

  public boolean isSuperclassOf(Class<?> interfaze) {
    if (constructor == null) {
      return false;
    }
    return interfaze.isAssignableFrom(constructor.getDeclaringClass());
  }
}
