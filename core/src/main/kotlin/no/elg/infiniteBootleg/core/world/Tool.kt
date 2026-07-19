package no.elg.infiniteBootleg.core.world

import no.elg.infiniteBootleg.core.assets.InfAssets
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.items.ItemType
import no.elg.infiniteBootleg.core.items.ToolItem
import no.elg.infiniteBootleg.core.items.ToolItemFist
import no.elg.infiniteBootleg.core.items.ToolItemImpl
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.sealedSubclassObjectInstances
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Element.ToolElement as ProtoToolElement

@Suppress("unused")
sealed interface Tool<DATA : ToolData> : TexturedContainerElement {
  override val itemType: ItemType get() = ItemType.TOOL
  override val stateless: Boolean get() = false

  /**
   *
   * @return If this tool can be handled by the player
   */
  override val canBeHandled: Boolean get() = true

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

  @Deprecated("Use the toItem function with data exposed", replaceWith = ReplaceWith("toItem(maxStock, stock, data)"))
  override fun toItem(maxStock: UInt, stock: UInt): Item

  fun toItem(maxStock: UInt, stock: UInt, data: DATA): ToolItem<DATA>

  object Pickaxe : Tool<PickaxeToolData> {
    override val textureName: String = InfAssets.PICKAXE_TEXTURE
    override val effectiveAgainst: Set<MaterialCategory> = setOf(MaterialCategory.ORE)
    override val destroyIneffectiveAgainst: Boolean get() = false
    override val effectiveEfficiency: Float get() = 1f
    override val ineffectiveEfficiency: Float get() = 0.75f

    @Deprecated("Use the toItem function with data exposed", replaceWith = ReplaceWith("toItem(maxStock, stock, PickaxeToolData())"))
    override fun toItem(maxStock: UInt, stock: UInt): ToolItem<PickaxeToolData> = toItem(maxStock, stock, PickaxeToolData())

    override fun toItem(maxStock: UInt, stock: UInt, data: PickaxeToolData): ToolItem<PickaxeToolData> = ToolItemImpl(this, maxStock, stock, data)
  }

  object Broadaxe : Tool<BroadaxeToolData> {
    override val textureName: String = InfAssets.BROADAXE_TEXTURE
    override val effectiveAgainst: Set<MaterialCategory> = setOf(MaterialCategory.PLAIN_ROCK, MaterialCategory.SOIL, MaterialCategory.ORGANIC)
    override val destroyIneffectiveAgainst: Boolean get() = true
    override val effectiveEfficiency: Float get() = 2f
    override val ineffectiveEfficiency: Float get() = 0.25f

    @Deprecated("Use the toItem function with data exposed", replaceWith = ReplaceWith("toItem(maxStock, stock, BroadaxeToolData())"))
    override fun toItem(maxStock: UInt, stock: UInt): ToolItem<BroadaxeToolData> = toItem(maxStock, stock, BroadaxeToolData())

    override fun toItem(maxStock: UInt, stock: UInt, data: BroadaxeToolData): ToolItem<BroadaxeToolData> = ToolItemImpl(this, maxStock, stock, data)
  }

  object Reclaimer : Tool<ReclaimerToolData> {
    override val textureName: String = InfAssets.RECLAIMER_TEXTURE
    override val effectiveAgainst: Set<MaterialCategory> = setOf(MaterialCategory.CRAFTED)
    override val destroyIneffectiveAgainst: Boolean get() = true
    override val effectiveEfficiency: Float get() = 3f
    override val ineffectiveEfficiency: Float get() = 0.01f

    @Deprecated("Use the toItem function with data exposed", replaceWith = ReplaceWith("toItem(maxStock, stock, ReclaimerToolData())"))
    override fun toItem(maxStock: UInt, stock: UInt): ToolItem<ReclaimerToolData> = toItem(maxStock, stock, ReclaimerToolData())

    override fun toItem(maxStock: UInt, stock: UInt, data: ReclaimerToolData): ToolItem<ReclaimerToolData> = ToolItemImpl(this, maxStock, stock, data)
  }

  object Fist : Tool<FistToolData> {
    override val effectiveAgainst: Set<MaterialCategory> = setOf(SOIL, PLAIN_ROCK, ORGANIC)
    override val destroyIneffectiveAgainst: Boolean get() = true
    override val effectiveEfficiency: Float get() = 0.1f
    override val ineffectiveEfficiency: Float get() = Float.MIN_VALUE
    override val canBeHandled: Boolean get() = false

    @Suppress("OVERRIDE_DEPRECATION") // not really deprecated here anymore
    override fun toItem(maxStock: UInt, stock: UInt): Item = ToolItemFist
    override fun toItem(maxStock: UInt, stock: UInt, data: FistToolData): ToolItem<FistToolData> = ToolItemFist

    override val textureName: String get() = InfAssets.FIST_TEXTURE
  }

  companion object {
    val tools: List<Tool<*>> = sealedSubclassObjectInstances<Tool<*>>()
    val normalTools: List<Tool<*>> = tools.filter(ContainerElement::canBeHandled)

    private val nameToTool: Map<String, Tool<*>> = tools.associateBy { it.javaClass.simpleName.lowercase() }
    private val toolToName: Map<Tool<*>, String> = tools.associateWith { it.javaClass.simpleName.lowercase() }

    fun nameOf(tool: Tool<*>): String = toolToName[tool] ?: error("Failed to find name for tool $tool")

    fun valueOfOrNull(name: String): Tool<*>? = nameToTool[name.lowercase()]

    fun valueOf(name: String): Tool<*> = valueOfOrNull(name) ?: error("Unknown tool with name '$name'")

    fun Sequence<WorldCompactLoc>.filterNotAirBlock(world: World): Sequence<WorldCompactLoc> = filterNot { world.isAirBlock(it, false) }

    fun ProtoToolElement.fromProto(): ToolData =
      when {
        hasPickaxe() -> PickaxeToolData.fromProto(this)
        hasReclaimer() -> ReclaimerToolData.fromProto(this)
        hasBroadaxe() -> BroadaxeToolData.fromProto(this)
        hasHand() -> FistToolData
        else -> error("Unknown toolData type: $this")
      }
  }
}
