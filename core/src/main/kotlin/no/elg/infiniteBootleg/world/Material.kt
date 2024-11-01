package no.elg.infiniteBootleg.world

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.items.ItemType
import no.elg.infiniteBootleg.items.MaterialItem
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.material
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.component1
import no.elg.infiniteBootleg.util.component2
import no.elg.infiniteBootleg.util.findTextures
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.util.serverRotatableTextureRegion
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.BlockImpl
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.api.ProtoConverter
import no.elg.infiniteBootleg.world.ecs.components.ExplosiveComponent
import no.elg.infiniteBootleg.world.ecs.components.required.EntityTypeComponent.Companion.entityTypeComponent
import no.elg.infiniteBootleg.world.ecs.creation.createBlockEntity
import no.elg.infiniteBootleg.world.ecs.creation.createContainerEntity
import no.elg.infiniteBootleg.world.ecs.creation.createDoorBlockEntity
import no.elg.infiniteBootleg.world.ecs.creation.createGravityAffectedBlockEntity
import no.elg.infiniteBootleg.world.ecs.creation.createLeafEntity
import no.elg.infiniteBootleg.world.ecs.explosiveBlockFamily
import no.elg.infiniteBootleg.world.ecs.load
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.world.World
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
   * @return If this material can be rotated, but the entity can handle the rendering
   */
  val invisibleBlock: Boolean = false,
  /**
   *
   * @return If this material can be handled by the player, otherwise this is a meta material
   */
  val canBeHandled: Boolean = true,
  private val createNew: ((world: World, chunk: Chunk, worldX: WorldCoord, worldY: WorldCoord, material: Material) -> CompletableFuture<Entity>)? = null
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
    createNew = { world: World, chunk: Chunk, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createBlockEntity(world, chunk, worldX, worldY, material, arrayOf(explosiveBlockFamily to "explosiveBlockFamily")) {
        safeWith { ExplosiveComponent() }
      }
    }
  ),
  SAND(hardness = 0.75f, hasTransparentTexture = false, createNew = { world: World, chunk: Chunk, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
    world.engine.createGravityAffectedBlockEntity(world, chunk, worldX, worldY, material)
  }),
  TORCH(
    hardness = 0.1f,
    hasTransparentTexture = true,
    isCollidable = false,
    blocksLight = false,
    emitsLight = true,
    createNew = { world: World, chunk: Chunk, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createGravityAffectedBlockEntity(world, chunk, worldX, worldY, material)
    }
  ),
  GLASS(hardness = 0.1f, hasTransparentTexture = true, blocksLight = false),
  DOOR(
    hardness = 1f,
    hasTransparentTexture = true,
    isCollidable = false,
    blocksLight = false,
    invisibleBlock = true,
    createNew = { world: World, chunk, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createDoorBlockEntity(world, chunk, worldX, worldY, material)
    }
  ),
  BIRCH_TRUNK(hardness = 1.25f, hasTransparentTexture = true, isCollidable = false, blocksLight = false),
  BIRCH_LEAVES(
    hardness = 0.1f,
    hasTransparentTexture = true,
    isCollidable = false,
    blocksLight = false,
    createNew = { world: World, chunk, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createLeafEntity(world, chunk, worldX, worldY, material)
    }
  ),
  SANDSTONE(
    hardness = 1f,
    hasTransparentTexture = false
  ),
  CONTAINER(hardness = 1f, hasTransparentTexture = false, isCollidable = false, createNew = { world: World, chunk, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
    world.engine.createContainerEntity(world, chunk, worldX, worldY, material)
  });

  override var textureRegion: RotatableTextureRegion? = null

  init {
    if (Settings.client) {
      textureRegion = this.findTextures(textureName)
      if (textureRegion == null && !canBeHandled) {
        throw NullPointerException("Failed to find a texture for $displayName")
      }
    } else {
      textureRegion = serverRotatableTextureRegion(textureName)
    }
  }

  /**
   * @param world World this block this exists in
   * @param chunk
   * @param localX Relative x in the chunk
   * @param localY Relative y in the chunk
   * @return A block of this type
   */
  fun createBlock(
    world: World,
    chunk: Chunk,
    localX: LocalCoord,
    localY: LocalCoord,
    protoEntity: ProtoWorld.Entity? = null
  ): Block {
    require(!chunk.isDisposed) { "Created block in disposed chunk" }
    return BlockImpl(chunk, localX, localY, this).also { block ->
      if (Main.isAuthoritative) {
        // Blocks client side should not have any entity in them
        val futureEntity = protoEntity?.let { world.load(it, chunk) } ?: createNew?.invoke(world, chunk, chunk.worldX + localX, chunk.worldY + localY, this)
        futureEntity?.thenApply { entity: Entity ->
          if (block.isDisposed) {
            world.removeEntity(entity)
            // This will fire when generating features in the world (i.e., trees next to other trees)
            logger.debug {
              "Block@${stringifyCompactLoc(block)} chunk ${stringifyCompactLoc(chunk)} was disposed" +
                " before entity (type ${entity.entityTypeComponent.hudDebug()}) was fully created. " +
                "Is the chunk disposed? ${chunk.isDisposed}"
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
      chunk.updateTexture(prioritize)
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

    override fun ProtoWorld.Material.fromProto(): Material = Material.entries[ordinal]

    fun fromOrdinal(ordinal: Int): Material = entries[ordinal]
  }
}
