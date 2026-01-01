package no.elg.infiniteBootleg.core.world

import no.elg.infiniteBootleg.core.items.ItemType
import no.elg.infiniteBootleg.core.items.ToolItem
import no.elg.infiniteBootleg.core.util.sealedSubclassObjectInstances

@Suppress("unused")
sealed interface Tool : TexturedContainerElement {
  override val itemType: ItemType get() = ItemType.TOOL
  override fun toItem(maxStock: UInt, stock: UInt): ToolItem = ToolItem(this, maxStock, stock)

  object Pickaxe : Tool {
    override val textureName: String = "pickaxe"
  }

  companion object {
    val tools: List<Tool> = sealedSubclassObjectInstances<Tool>()

    private val nameToTool: Map<String, Tool> = tools.associateBy { it.javaClass.simpleName.lowercase() }
    private val toolToName: Map<Tool, String> = tools.associateWith { it.javaClass.simpleName.lowercase() }

    fun nameOf(tool: Tool): String = toolToName[tool] ?: error("Failed to find name for tool $tool")

    fun valueOfOrNull(name: String): Tool? = nameToTool[name.lowercase()]

    fun valueOf(name: String): Tool = valueOfOrNull(name) ?: error("Unknown tool with name '$name'")
  }
}
