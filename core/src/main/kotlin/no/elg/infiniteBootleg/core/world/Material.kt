package no.elg.infiniteBootleg.core.world

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.longs.LongIterators
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
import it.unimi.dsi.fastutil.longs.LongIterator as FastUtilLongIterator

private val logger = KotlinLogging.logger {}

@Suppress("unused")
sealed interface Material : ContainerElement {
  /**
   * How hard it is to break this material
   */
  val hardness: Float

  /**
   * @return If the texture of the material has any transparency
   */
  val hasTransparentTexture: Boolean get() = false

  /**
   * @return If this material can be collided with
   */
  val isCollidable: Boolean get() = true

  /**
   * @return If this material blocks light
   */
  val blocksLight: Boolean get() = true

  /**
   * @return If this material emits light
   */
  val emitsLight: Boolean get() = false

  /**
   * @return If this material has no texture
   */
  val invisibleBlock: Boolean get() = false

  /**
   *
   * @return If this material can be handled by the player, otherwise this is a _meta material_
   */
  val canBeHandled: Boolean get() = true

  /**
   * @return The entity to attach to this block
   */
  val createNew: ((World, WorldCoord, WorldCoord, Material) -> CompletableFuture<Entity>)? get() = null

  override val itemType: ItemType get() = ItemType.BLOCK
  override fun toItem(maxStock: UInt, stock: UInt): MaterialItem = MaterialItem(this, maxStock, stock)
//  val textureName: String? get() = if (canBeHandled) if (customTextureName != null) customTextureName else this::class.simpleName. else null

  object Air : Material {
    override val hardness get() = 0f
    override val hasTransparentTexture get() = true
    override val isCollidable get() = false
    override val blocksLight get() = false
    override val invisibleBlock get() = true
    override val canBeHandled get() = false
  }

  object Stone : Material, TexturedContainerElement {
    override val hardness get() = 1.5f
    override val textureName: String get() = "stone"
  }

  object Brick : Material, TexturedContainerElement {
    override val hardness get() = 2f
    override val textureName: String get() = "brick"
  }

  object Dirt : Material, TexturedContainerElement {
    override val hardness get() = 1f
    override val textureName: String get() = "dirt"
  }

  object Grass : Material, TexturedContainerElement {
    override val hardness get() = 0.8f
    override val textureName: String get() = "grass"
  }

  object Tnt : Material, TexturedContainerElement {
    override val hardness get() = 0.5f
    override val textureName: String get() = "tnt"
    override val createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createBlockEntity(world, worldX, worldY, material, arrayOf(explosiveBlockFamily to "explosiveBlockFamily")) {
        safeWith { ExplosiveComponent() }
      }
    }
  }

  object Sand : Material, TexturedContainerElement {
    override val hardness get() = 0.75f
    override val textureName: String get() = "sand"
    override val hasTransparentTexture: Boolean get() = false
    override val createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createGravityAffectedBlockEntity(world, worldX, worldY, material)
    }
  }

  object Torch : Material, TexturedContainerElement {
    override val hardness get() = 0.1f
    override val textureName: String get() = "torch"
    override val hasTransparentTexture get() = true
    override val isCollidable get() = false
    override val blocksLight get() = false
    override val emitsLight get() = true
    override val createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createGravityAffectedBlockEntity(world, worldX, worldY, material)
    }
  }

  object Glass : Material, TexturedContainerElement {
    override val hardness get() = 0.1f
    override val textureName: String get() = "glass"
    override val hasTransparentTexture get() = true
    override val blocksLight get() = false
  }

  object Door : Material, TexturedContainerElement {
    override val hardness get() = 1f
    override val textureName: String get() = "door"
    override val hasTransparentTexture get() = true
    override val isCollidable get() = false
    override val blocksLight get() = false
    override val invisibleBlock get() = true
    override val createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createDoorBlockEntity(world, worldX, worldY, material)
    }
  }

  object BirchTrunk : Material, TexturedContainerElement {
    override val hardness get() = 1.25f
    override val textureName: String get() = "birch_trunk"
    override val hasTransparentTexture get() = true
    override val isCollidable get() = false
    override val blocksLight get() = false
  }

  object BirchLeaves : Material, TexturedContainerElement {
    override val hardness = 0.1f
    override val textureName: String get() = "birch_leaves"
    override val hasTransparentTexture = true
    override val isCollidable = false
    override val blocksLight = false
    override val createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createLeafEntity(world, worldX, worldY, material)
    }
  }

  object Sandstone : Material, TexturedContainerElement {
    override val hardness = 1f
    override val textureName: String get() = "sandstone"
  }

  object Container : Material, TexturedContainerElement {
    override val hardness = 1f
    override val textureName: String get() = "container"
    override val createNew = { world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material ->
      world.engine.createContainerEntity(world, worldX, worldY, material)
    }
  }

  object CoalOre : Material, TexturedContainerElement {
    override val hardness = 1.25f
    override val textureName: String get() = "coal_ore"
  }

  object CopperOre : Material, TexturedContainerElement {
    override val hardness = 2f
    override val textureName: String get() = "copper_ore"
  }

  object IronOre : Material, TexturedContainerElement {
    override val hardness = 3f
    override val textureName: String get() = "iron_ore"
  }

  object GoldOre : Material, TexturedContainerElement {
    override val hardness = 1f
    override val textureName: String get() = "gold_ore"
  }

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
      if (Main.isAuthoritative) {
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

  fun createBlocks(world: World, locs: LongArray, prioritize: Boolean = true, allowOverwriteNonAir: Boolean = false) =
    createBlocks(world, LongIterators.wrap(locs), prioritize, allowOverwriteNonAir)

  fun createBlocks(world: World, locs: Iterable<Long>, prioritize: Boolean = true, allowOverwriteNonAir: Boolean = false) =
    createBlocks(world, LongIterators.asLongIterator(locs.iterator()), prioritize, allowOverwriteNonAir)

  fun createBlocks(world: World, locs: FastUtilLongIterator, prioritize: Boolean = true, allowOverwriteNonAir: Boolean = false) {
    val chunks = mutableSetOf<Chunk>()
    for ((worldX, worldY) in locs) {
      if (allowOverwriteNonAir || world.isAirBlock(worldX, worldY, markerIsAir = false)) {
        val block = world.setBlock(worldX, worldY, this, false, prioritize)
        chunks += block?.chunk ?: continue
      }
    }
    for (chunk in chunks) {
      chunk.dirty(prioritize)
    }
  }

  companion object : ProtoConverter<Material, ProtoWorld.Material> {

    val materials: List<Material> = Material::class.sealedSubclasses.map { it.objectInstance ?: error("Material ${it.simpleName} is not an object") }

    /**
     * All materials that can be interacted in a normal fashion by the player
     */
    val normalMaterials: List<Material> = materials.filter(Material::canBeHandled)

    private val nameToMaterial: Map<String, Material> =
      materials.associateBy { it.javaClass.simpleName.lowercase() } + mapOf("" to Air)

    private val materialToName: Map<Material, String> = materials.associateWith { it.javaClass.simpleName.lowercase() }

    fun nameOf(material: Material): String = materialToName[material] ?: error("Failed to find name for material $material")

    fun valueOfOrNull(name: String): Material? = nameToMaterial[name.lowercase()]

    fun valueOf(name: String): Material = valueOfOrNull(name) ?: error("Unknown material with name '$name'")

    override fun Material.asProto(): ProtoWorld.Material =
      material {
        name = if (this@asProto === Air) "" else nameOf(this@asProto)
      }

    override fun ProtoWorld.Material.fromProto(): Material = valueOf(this@fromProto.name)
  }
}
