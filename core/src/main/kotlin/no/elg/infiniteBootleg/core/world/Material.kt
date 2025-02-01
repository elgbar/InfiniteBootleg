package no.elg.infiniteBootleg.core.world

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.items.ItemType
import no.elg.infiniteBootleg.core.items.MaterialItem
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.util.stringifyCompactLocWithChunk
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.BlockImpl
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.ecs.api.ProtoConverter
import no.elg.infiniteBootleg.core.world.ecs.components.ExplosiveComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.EntityTypeComponent.Companion.entityTypeComponent
import no.elg.infiniteBootleg.core.world.ecs.creation.createBlockEntity
import no.elg.infiniteBootleg.core.world.ecs.creation.createContainerEntity
import no.elg.infiniteBootleg.core.world.ecs.creation.createDoorBlockEntity
import no.elg.infiniteBootleg.core.world.ecs.creation.createGravityAffectedBlockEntity
import no.elg.infiniteBootleg.core.world.ecs.creation.createLeafEntity
import no.elg.infiniteBootleg.core.world.ecs.explosiveBlockFamily
import no.elg.infiniteBootleg.core.world.ecs.load
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.material
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
enum class Material(
  /**
   * How hard it is to break this material
   */
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
   * @return If this material has no texture
   */
  val invisibleBlock: Boolean = false,
  /**
   *
   * @return If this material can be handled by the player, otherwise this is a _meta material_
   */
  val canBeHandled: Boolean = true,
  private val createNew: ((World, WorldCoord, WorldCoord, Material) -> CompletableFuture<Entity>)? = null
) : ContainerElement {
  AIR(
    hardness = 0f,
    hasTransparentTexture = true,
    isCollidable = false,
    blocksLight = false,
    emitsLight = false,
    invisibleBlock = true,
    canBeHandled = false
  ),
  STONE(hardness = 1.5f, hasTransparentTexture = false),
  BRICK(hardness = 2f, hasTransparentTexture = false),
  DIRT(
    hardness = 1f,
    hasTransparentTexture = false
  ),
  GRASS(hardness = 0.8f, hasTransparentTexture = false),
  TNT(
    hardness = 0.5f,
    hasTransparentTexture = false,
    createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createBlockEntity(world, worldX, worldY, material, arrayOf(explosiveBlockFamily to "explosiveBlockFamily")) {
        safeWith { ExplosiveComponent() }
      }
    }
  ),
  SAND(hardness = 0.75f, hasTransparentTexture = false, createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
    world.engine.createGravityAffectedBlockEntity(world, worldX, worldY, material)
  }),
  TORCH(
    hardness = 0.1f,
    hasTransparentTexture = true,
    isCollidable = false,
    blocksLight = false,
    emitsLight = true,
    createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createGravityAffectedBlockEntity(world, worldX, worldY, material)
    }
  ),
  GLASS(hardness = 0.1f, hasTransparentTexture = true, blocksLight = false),
  DOOR(
    hardness = 1f,
    hasTransparentTexture = true,
    isCollidable = false,
    blocksLight = false,
    invisibleBlock = true,
    createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createDoorBlockEntity(world, worldX, worldY, material)
    }
  ),
  BIRCH_TRUNK(hardness = 1.25f, hasTransparentTexture = true, isCollidable = false, blocksLight = false),
  BIRCH_LEAVES(
    hardness = 0.1f,
    hasTransparentTexture = true,
    isCollidable = false,
    blocksLight = false,
    createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createLeafEntity(world, worldX, worldY, material)
    }
  ),
  SANDSTONE(
    hardness = 1f,
    hasTransparentTexture = false
  ),
  CONTAINER(hardness = 1f, hasTransparentTexture = false, isCollidable = false, createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
    world.engine.createContainerEntity(world, worldX, worldY, material)
  });

  /**
   * @param world World this block this exists in
   * @param chunk
   * @param localX Relative x in the chunk
   * @param localY Relative y in the chunk
   * @param tryRevalidateChunk If the chunk is disposed, should it be fetched from the world?
   * @return A block of this type
   */
  fun createBlock(
    world: World,
    chunk: Chunk,
    localX: LocalCoord,
    localY: LocalCoord,
    protoEntity: ProtoWorld.Entity? = null,
    tryRevalidateChunk: Boolean = true
  ): Block {
    val validChunk = if (chunk.isDisposed && tryRevalidateChunk) world.getChunk(chunk.compactLocation) else chunk
    requireNotNull(validChunk) { "No valid chunk found" }
    require(validChunk.isNotDisposed) { "Chunk has been disposed" }
    return BlockImpl(validChunk, localX, localY, this).also { block ->
      if (Main.Companion.isAuthoritative) {
        // Blocks client side should not have any entity in them
        val futureEntity = protoEntity?.let { world.load(it, validChunk) } ?: createNew?.invoke(world, validChunk.worldX + localX, validChunk.worldY + localY, this)
        futureEntity?.thenApply { entity: Entity ->
          if (block.isDisposed || validChunk.isDisposed) {
            world.removeEntity(entity)
            // This will fire when generating features in the world (i.e., trees next to other trees)
            logger.debug {
              "Block@${stringifyCompactLocWithChunk(block)} was disposed" +
                " before entity (type ${entity.entityTypeComponent.hudDebug()}) was fully created. " +
                "Is the chunk disposed? ${validChunk.isDisposed}, block disposed? ${block.isDisposed}"
            }
          } else {
            block.entity = entity
          }
        }
      }
    }
  }

  fun createBlocks(world: World, locs: LongArray, prioritize: Boolean = true, allowOverwiteNonAir: Boolean = false) {
    createBlocks(world, locs.asIterable(), prioritize, allowOverwiteNonAir)
  }

  fun createBlocks(world: World, locs: Iterable<Long>, prioritize: Boolean = true, allowOverwiteNonAir: Boolean = false) {
    val chunks = mutableSetOf<Chunk>()
    for ((worldX, worldY) in locs) {
      if (allowOverwiteNonAir || world.isAirBlock(worldX, worldY, markerIsAir = false)) {
        val block = world.setBlock(worldX, worldY, this, false, prioritize)
        chunks += block?.chunk ?: continue
      }
    }
    for (chunk in chunks) {
      chunk.dirty(prioritize)
    }
  }

  override val itemType: ItemType get() = ItemType.BLOCK

  override fun toItem(maxStock: UInt, stock: UInt): MaterialItem = MaterialItem(this, maxStock, stock)

  companion object : ProtoConverter<Material, ProtoWorld.Material> {

    /**
     * All materials that can be interacted in a normal fashion by the player
     */
    val normalMaterials: List<Material> = entries.filter(Material::canBeHandled)

    override fun Material.asProto(): ProtoWorld.Material =
      material {
        ordinal = this@asProto.ordinal
      }

    override fun ProtoWorld.Material.fromProto(): Material = entries[ordinal]

    fun fromOrdinal(ordinal: Int): Material = entries[ordinal]
  }
}
