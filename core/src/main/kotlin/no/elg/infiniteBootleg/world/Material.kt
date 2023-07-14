package no.elg.infiniteBootleg.world

import com.badlogic.ashley.core.Entity
import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.KAssets.textureAtlas
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.items.ItemType
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.world.ecs.components.block.ExplosiveComponent
import no.elg.infiniteBootleg.world.ecs.createBlockEntity
import no.elg.infiniteBootleg.world.ecs.createDoorBlockEntity
import no.elg.infiniteBootleg.world.ecs.createGravityAffectedBlockEntity
import no.elg.infiniteBootleg.world.ecs.explosiveBlockFamily
import no.elg.infiniteBootleg.world.ecs.with
import no.elg.infiniteBootleg.world.render.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.RotatableTextureRegion.Companion.allowedRotation
import no.elg.infiniteBootleg.world.render.RotatableTextureRegion.Companion.disallowedRotation
import no.elg.infiniteBootleg.world.world.World
import java.util.Locale

/**
 * @author Elg
 */
enum class Material(
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
  val isGravityAffected: Boolean = false,
  val createNew: ((world: World, chunk: Chunk, worldX: Int, worldY: Int, material: Material) -> Entity)? = null
) {
  AIR(itemType = ItemType.AIR, hardness = 0f, hasTransparentTexture = true, isCollidable = false, blocksLight = false, emitsLight = false, adjacentPlaceable = false),
  STONE(hardness = 1.5f, hasTransparentTexture = false),
  BRICK(hardness = 2f, hasTransparentTexture = false),
  DIRT(hardness = 1f, hasTransparentTexture = false),
  GRASS(hardness = 0.8f, hasTransparentTexture = false),
  TNT(hardness = 0.5f, hasTransparentTexture = false, createNew = { world: World, chunk: Chunk, worldX: Int, worldY: Int, material: Material ->
    world.engine.createBlockEntity(world, chunk, worldX, worldY, material, arrayOf(explosiveBlockFamily to "explosiveBlockFamily")) {
      with(ExplosiveComponent())
    }
  }),
  SAND(hardness = 1f, hasTransparentTexture = false, isGravityAffected = true, createNew = { world: World, chunk: Chunk, worldX: Int, worldY: Int, material: Material ->
    world.engine.createGravityAffectedBlockEntity(world, chunk, worldX, worldY, material)
  }),
  TORCH(
    hardness = 0.1f,
    hasTransparentTexture = true,
    isCollidable = false,
    blocksLight = false,
    emitsLight = true,
    adjacentPlaceable = false,
    isGravityAffected = true,
    createNew = { world: World, chunk: Chunk, worldX: Int, worldY: Int, material: Material ->
      world.engine.createGravityAffectedBlockEntity(world, chunk, worldX, worldY, material)
    }
  ),
  GLASS(hardness = 0.1f, hasTransparentTexture = true, blocksLight = false),
  DOOR(
    itemType = ItemType.BLOCK,
    hardness = 1f,
    hasTransparentTexture = true,
    isCollidable = false,
    blocksLight = false,
    createNew = { world: World, chunk, worldX: Int, worldY: Int, material: Material ->
      world.engine.createDoorBlockEntity(world, chunk, worldX, worldY, material)
    }
  ),
  BIRCH_TRUNK(hardness = 1.25f, hasTransparentTexture = true, isCollidable = false, blocksLight = false),
  BIRCH_LEAVES(hardness = 0.1f, hasTransparentTexture = true, isCollidable = false, blocksLight = false, adjacentPlaceable = false);

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
    val entity = createNew?.invoke(world, chunk, chunk.worldX + localX, chunk.worldY + localY, this)
    return BlockImpl(world, chunk, localX, localY, this, entity)
  }

  fun createBlocks(world: World, locs: Iterable<Long>, prioritize: Boolean = true) {
    check(isBlock) { "This material ($name) is not a block" }
    val chunks = mutableSetOf<Chunk>()
    for ((worldX, worldY) in locs) {
      val currentMaterial = world.getMaterial(worldX, worldY)
      if (currentMaterial == AIR) {
        val block = world.setBlock(worldX, worldY, this, false, prioritize)
        chunks += block?.chunk ?: continue
      }
    }
    for (chunk in chunks) {
      chunk.updateTexture(prioritize)
    }
  }

  val isBlock: Boolean
    get() = itemType == ItemType.BLOCK || itemType == ItemType.AIR

  val isEntity: Boolean
    get() = itemType == ItemType.ENTITY

  companion object {
    private val VALUES = values()

    @JvmStatic
    fun fromOrdinal(b: Int): Material {
      return VALUES[b]
    }
  }
}
