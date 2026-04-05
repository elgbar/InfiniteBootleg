package no.elg.infiniteBootleg.core.world

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.items.ItemType
import no.elg.infiniteBootleg.core.items.ToolItem
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.centerOfBlock
import no.elg.infiniteBootleg.core.util.interactableBlocksWithinRadius
import no.elg.infiniteBootleg.core.util.sealedSubclassObjectInstances
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.core.world.world.World.Companion.getLocationsAABBFromLowerLeftCorner
import kotlin.math.floor

@Suppress("unused")
sealed interface Tool : TexturedContainerElement {
  override val itemType: ItemType get() = ItemType.TOOL
  override fun toItem(maxStock: UInt, stock: UInt): ToolItem = ToolItem(this, maxStock, stock)

  /**
   * @param entity Which entity is using this tool
   * @param world In which world are this entity using the tool
   * @param blockX Origin of the breaking. Typically, where the mouse is pointing
   * @param blockY Origin of the breaking. Typically, where the mouse is pointing
   * @param size How large the breaking is
   * @param interactionRadius Max size of interaction from the entities position
   */
  fun breakableLocs(
    entity: Entity,
    world: World,
    blockX: WorldCoord,
    blockY: WorldCoord,
    size: Float,
    interactionRadius: Float
  ): Sequence<WorldCompactLoc>

  /**
   * The categories this tool works better on
   */
  val effectiveAgainst: Set<MaterialCategory>

  /**
   * Should we just destroy the materials this tool is not effective against? I.e., not give it to the entity
   */
  val destroyIneffectiveAgainst: Boolean

  /**
   * How effective this tool is against blocks that is in [effectiveAgainst]
   */
  val effectiveEfficiency: Float

  /**
   * How effective this tool is against blocks that is NOT in [effectiveAgainst]
   */
  val ineffectiveEfficiency: Float

  object Pickaxe : Tool {
    override val textureName: String = "pickaxe"
    override val effectiveAgainst: Set<MaterialCategory> = setOf(MaterialCategory.ORE)
    override val destroyIneffectiveAgainst: Boolean get() = false
    override val effectiveEfficiency: Float get() = 1f
    override val ineffectiveEfficiency: Float get() = 0.75f

    override fun breakableLocs(
      entity: Entity,
      world: World,
      blockX: WorldCoord,
      blockY: WorldCoord,
      size: Float,
      interactionRadius: Float
    ): Sequence<WorldCompactLoc> {
      val baseSeq = World.getLocationsWithin(blockX, blockY, size).asSequence()
      return entity
        .interactableBlocksWithinRadius(world, interactionRadius, baseSeq)
        .filterNotAirBlock(world)
    }
  }

  object Broadaxe : Tool {
    override val textureName: String = "papp"
    override val effectiveAgainst: Set<MaterialCategory> = setOf(MaterialCategory.PLAIN_ROCK, MaterialCategory.SOIL, MaterialCategory.ORGANIC)
    override val destroyIneffectiveAgainst: Boolean get() = true
    override val effectiveEfficiency: Float get() = 2f
    override val ineffectiveEfficiency: Float get() = 0.25f

    private const val OFFSET_Y: Double = 3 - 1.0 // always 3 blocks high, to calculate offset we remove one

    override fun breakableLocs(
      entity: Entity,
      world: World,
      blockX: WorldCoord,
      blockY: WorldCoord,
      size: Float,
      interactionRadius: Float
    ): Sequence<WorldCompactLoc> {
      val ifLeftOfEntity = entity.positionComponent.x <= blockX.centerOfBlock()
      val leftWorldX = blockX.toDouble()
      val offsetX = when {
        size == 1f -> 0.0
        ifLeftOfEntity -> floor(size) - 1.0
        else -> -floor(size) + 1.0
      }

      val locationsAABBFromCorner = getLocationsAABBFromLowerLeftCorner(
        leftWorldX,
        blockY.toDouble() - (OFFSET_Y / 2.0),
        offsetX,
        OFFSET_Y
      )
      return locationsAABBFromCorner.asSequence().filterNotAirBlock(world)
    }
  }

  object Reclaimer : Tool {
    override val textureName: String = "hand"
    override val effectiveAgainst: Set<MaterialCategory> = setOf(MaterialCategory.CRAFTED)
    override val destroyIneffectiveAgainst: Boolean get() = true
    override val effectiveEfficiency: Float get() = 3f
    override val ineffectiveEfficiency: Float get() = 0.01f

    override fun breakableLocs(
      entity: Entity,
      world: World,
      blockX: WorldCoord,
      blockY: WorldCoord,
      size: Float,
      interactionRadius: Float
    ): Sequence<WorldCompactLoc> {
      val baseSeq = World.getLocationsWithin(blockX, blockY, size).asSequence()
      return entity
        .interactableBlocksWithinRadius(world, interactionRadius, baseSeq)
        .filterNotAirBlock(world)
    }
  }

  companion object {
    val tools: List<Tool> = sealedSubclassObjectInstances<Tool>()

    private val nameToTool: Map<String, Tool> = tools.associateBy { it.javaClass.simpleName.lowercase() }
    private val toolToName: Map<Tool, String> = tools.associateWith { it.javaClass.simpleName.lowercase() }

    fun nameOf(tool: Tool): String = toolToName[tool] ?: error("Failed to find name for tool $tool")

    fun valueOfOrNull(name: String): Tool? = nameToTool[name.lowercase()]

    fun valueOf(name: String): Tool = valueOfOrNull(name) ?: error("Unknown tool with name '$name'")

    private fun Sequence<WorldCompactLoc>.filterNotAirBlock(world: World): Sequence<WorldCompactLoc> = filterNot { world.isAirBlock(it, false) }
  }
}
