package no.elg.infiniteBootleg.world

import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.KAssets.textureAtlas
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.items.ItemType
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.world.blocks.FallingBlock
import no.elg.infiniteBootleg.world.blocks.TickingBlock
import no.elg.infiniteBootleg.world.blocks.TntBlock
import no.elg.infiniteBootleg.world.blocks.Torch
import no.elg.infiniteBootleg.world.ecs.createDoorEntity
import no.elg.infiniteBootleg.world.render.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.RotatableTextureRegion.Companion.allowedRotation
import no.elg.infiniteBootleg.world.render.RotatableTextureRegion.Companion.disallowedRotation
import no.elg.infiniteBootleg.world.world.World
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.Locale

/**
 * @author Elg
 */
enum class Material(
  impl: Class<*>? = null,
  private val itemType: ItemType = ItemType.BLOCK,
  val hardness: Float,
  val textureName: String? = null,
  /**
   * @return If the texture of the material has any transparency
   */
  val hasTransparentTexture: Boolean,
  /**
   * @return If this material can be collided with
   */
  val isCollidable: Boolean = true,
  /**
   * @return If this material blocks light
   */
  val blocksLight: Boolean = true,
  /**
   * @return If this material emits light
   */
  val emitsLight: Boolean = false,
  /**
   * @return If this material can be used to place blocks near it
   */
  val adjacentPlaceable: Boolean = true,
  val createNew: ((world: World, worldX: Int, worldY: Int) -> Any)? = null
) {
  AIR(itemType = ItemType.AIR, hardness = 0f, hasTransparentTexture = true, isCollidable = false, blocksLight = false, emitsLight = false, adjacentPlaceable = false),
  STONE(hardness = 1.5f, hasTransparentTexture = false),
  BRICK(hardness = 2f, hasTransparentTexture = false),
  DIRT(hardness = 1f, hasTransparentTexture = false),
  GRASS(hardness = 0.8f, hasTransparentTexture = false),
  TNT(impl = TntBlock::class.java, hardness = 0.5f, hasTransparentTexture = false, emitsLight = true),
  SAND(impl = FallingBlock::class.java, hardness = 1f, hasTransparentTexture = false),
  TORCH(impl = Torch::class.java, hardness = 0.1f, hasTransparentTexture = true, isCollidable = false, blocksLight = false, emitsLight = true, adjacentPlaceable = false),
  GLASS(hardness = 0.1f, hasTransparentTexture = true, blocksLight = false),
  DOOR(
    itemType = ItemType.ENTITY,
    hardness = 1f,
    hasTransparentTexture = true,
    isCollidable = false,
    blocksLight = false,
    createNew = { world: World, worldX: Int, worldY: Int ->
      world.engine.createDoorEntity(world, worldX.toFloat(), worldY.toFloat())
    }
  ),
  BIRCH_TRUNK(hardness = 1.25f, hasTransparentTexture = true, isCollidable = false, blocksLight = false),
  BIRCH_LEAVES(hardness = 0.1f, hasTransparentTexture = false, isCollidable = false, blocksLight = false, adjacentPlaceable = false);

  private val constructor: Constructor<*>?
  private val constructorProtoBuf: Constructor<*>?

  var textureRegion: RotatableTextureRegion? = null

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
    if (impl != null && itemType == ItemType.BLOCK) {
      Preconditions.checkArgument(Block::class.java.isAssignableFrom(impl), "$name does not have ${Block::class.java.simpleName} as a super type")
      try {
        constructor = impl.getDeclaredConstructor(World::class.java, Chunk::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Material::class.java)
        constructorProtoBuf = null
      } catch (e: NoSuchMethodException) {
        throw IllegalStateException("There is no constructor of ${impl.simpleName} with the arguments World, Chunk, int, int, Material")
      }
    } else {
      constructor = null
      constructorProtoBuf = null
    }

    if (Settings.client && itemType != ItemType.AIR) {
      val textureName = textureName ?: name.lowercase(Locale.getDefault())
      textureRegion = textureAtlas.findRegion("${textureName}_rotatable")?.allowedRotation() ?: textureAtlas.findRegion(textureName).disallowedRotation()
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

  fun create(world: World, worldX: Int, worldY: Int, prioritize: Boolean = true, updateTexture: Boolean = true): Boolean {
    val currentMaterial = world.getMaterial(worldX, worldY)
    if (currentMaterial != this && currentMaterial == AIR) {
      if (isBlock) {
        val block = world.setBlock(worldX, worldY, this, updateTexture, prioritize)
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

  fun create(world: World, locs: Iterable<Long>, prioritize: Boolean = true) {
    if (isBlock) {
      createBlocks(world, locs, prioritize)
    } else if (isEntity) {
      createEntities(world, locs, prioritize)
    }
  }

  fun createEntities(world: World, locs: Iterable<Long>, prioritize: Boolean = true) {
    for ((worldX, worldY) in locs) {
      createNew?.let { it(world, worldX, worldY) } ?: error("Material with type entity does not have a createNew method")
    }
  }

  fun createBlocks(world: World, locs: Iterable<Long>, prioritize: Boolean = true) {
    check(isBlock) { "This material ($name) is not a block" }
    val chunks = mutableSetOf<Chunk>()
    for ((worldX, worldY) in locs) {
      val currentMaterial = world.getMaterial(worldX, worldY)
      if (currentMaterial != this && currentMaterial == AIR) {
        val block = world.setBlock(worldX, worldY, this, false, prioritize)
        (block as? TickingBlock)?.delayedShouldTick(1L)
        chunks += block?.chunk ?: continue
      }
    }
    for (chunk in chunks) {
      chunk.updateTexture(prioritize)
    }
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
