package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.items.ItemType
import no.elg.infiniteBootleg.items.ToolItem

enum class Tool(val textureName: String? = null) : ContainerElement {
  PICKAXE;

  override fun toItem(maxStock: UInt, stock: UInt): ToolItem = ToolItem(this, maxStock, stock)

  override val itemType: ItemType get() = ItemType.TOOL

  companion object {
    fun fromOrdinal(ordinal: Int): Tool = Tool.entries[ordinal]
  }
}
