package no.elg.infiniteBootleg.world

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.KAssets.blockAtlas
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.items.ItemType
import no.elg.infiniteBootleg.world.blocks.FallingBlock
import no.elg.infiniteBootleg.world.blocks.TickingBlock
import no.elg.infiniteBootleg.world.blocks.TntBlock
import no.elg.infiniteBootleg.world.blocks.Torch
import no.elg.infiniteBootleg.world.ecs.createDoorEntity
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.Locale

/**
 * @author Elg
 */
enum class Material(
  val hardness: Float,
  /**
   * @return If the texture of the material has any transparency
   */
  val isTransparent: Boolean,
  impl: Class<*>? = null,
  private val itemType: ItemType = ItemType.BLOCK,
  val isSolid: Boolean = true,
  val isBlocksLight: Boolean = true,
  val isPlacable: Boolean = true,
  /**
   * @return If this material emits light
   */
  val isLuminescent: Boolean = false,
  val createNew: ((world: World, worldX: Int, worldY: Int) -> Any)? = null
) {
  AIR(hardness = 0f, isTransparent = true, itemType = ItemType.AIR, isSolid = false, isBlocksLight = false, isPlacable = false, isLuminescent = false),
  STONE(hardness = 1.5f, isTransparent = false),
  BRICK(hardness = 2f, isTransparent = false),
  DIRT(hardness = 1f, isTransparent = false),
  GRASS(hardness = 0.8f, isTransparent = false),
  TNT(hardness = 0.5f, impl = TntBlock::class.java, isLuminescent = true, isTransparent = false),
  SAND(hardness = 1f, impl = FallingBlock::class.java, isTransparent = false),
  TORCH(hardness = 0.1f, impl = Torch::class.java, isSolid = false, isBlocksLight = false, isTransparent = true, isLuminescent = true),
  GLASS(hardness = 0.1f, isTransparent = true, isBlocksLight = false),
  DOOR(hardness = 1f, itemType = ItemType.ENTITY, isTransparent = true, isBlocksLight = false, isSolid = false, createNew = { world: World, worldX: Int, worldY: Int ->
    world.engine.createDoorEntity(world, worldX.toFloat(), worldY.toFloat())
  });

  private val constructor: Constructor<*>?
  private val constructorProtoBuf: Constructor<*>?

  var textureRegion: TextureRegion? = null

  /**
   * @param impl The implementation a block of this material must have
   * @param itemType
   * @param hardness
   * @param solid If objects can pass through this material
   * @param blocksLight If this material will block light
   * @param placable If a block of this material can be placed by a player
   * @param transparent
   */
  init {
    if (impl != null) {
      if (itemType == ItemType.BLOCK) {
        Preconditions.checkArgument(
          Block::class.java.isAssignableFrom(impl),
          name + " does not have " + Block::class.java.simpleName + " as a super type"
        )
        try {
          constructor = impl.getDeclaredConstructor(World::class.java, Chunk::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Material::class.java)
          constructorProtoBuf = null
        } catch (e: NoSuchMethodException) {
          throw IllegalStateException(
            "There is no constructor of " +
              impl.simpleName +
              " with the arguments World, Chunk, int, int, Material"
          )
          constructor = null
          constructorProtoBuf = null
        }
      } else {
        constructor = null
        constructorProtoBuf = null
      }
    } else {
      constructor = null
      constructorProtoBuf = null
    }

    if (Settings.client && itemType != ItemType.AIR) {
      textureRegion = blockAtlas.findRegion(name.lowercase(Locale.getDefault()))
      if (textureRegion == null) {
        throw NullPointerException("Failed to find a texture for $name")
      }
    } else {
      textureRegion = null
    }
  }

  /**
   * @param world World this block this exists in
   * @param chunk
   * @param localX Relative x in the chunk
   * @param localY Relative y in the chunk
   * @return A block of this type
   */
  fun createBlock(world: World, chunk: Chunk, localX: Int, localY: Int): Block {
    Preconditions.checkArgument(isBlock)
    return if (constructor == null) {
      BlockImpl(world, chunk, localX, localY, this)
    } else {
      try {
        constructor.newInstance(world, chunk, localX, localY, this) as Block
      } catch (e: InstantiationException) {
        throw IllegalStateException(e)
      } catch (e: IllegalAccessException) {
        throw IllegalStateException(e)
      } catch (e: InvocationTargetException) {
        throw IllegalStateException(e)
      }
    }
  }

  val isBlock: Boolean
    get() = itemType == ItemType.BLOCK || itemType == ItemType.AIR

  fun create(world: World, worldX: Int, worldY: Int, prioritize: Boolean = true): Boolean {
    val currentMaterial = world.getMaterial(worldX, worldY)
    if (currentMaterial != this && currentMaterial == AIR) {
      if (isBlock) {
        val block = world.setBlock(worldX, worldY, this, true, prioritize)
        (block as? TickingBlock)?.delayedShouldTick(1L)
        return block != null
      } else if (isEntity) {
        createNew?.let { it(world, worldX, worldY) } ?: error("Material with type entity does not have a createNew method")
        return true
      }
      throw IllegalStateException("This material ($name) is neither a block nor an entity")
    }
    return false
  }

  val isEntity: Boolean
    get() = itemType == ItemType.ENTITY

  fun isSuperclassOf(interfaze: Class<*>): Boolean {
    return if (constructor == null) {
      false
    } else {
      interfaze.isAssignableFrom(constructor.getDeclaringClass())
    }
  }

  companion object {
    private val VALUES = values()

    @JvmStatic
    fun fromOrdinal(b: Int): Material {
      return VALUES[b]
    }
  }
}
