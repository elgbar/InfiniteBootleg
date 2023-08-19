package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.items.InventoryElement
import no.elg.infiniteBootleg.items.ItemType
import no.elg.infiniteBootleg.items.ToolItem
import no.elg.infiniteBootleg.util.findTextures

enum class Tool(textureName: String? = null) : InventoryElement {
  PICKAXE;

  override val textureRegion = findTextures(textureName)

  override fun toItem(maxStock: UInt, stock: UInt): ToolItem = ToolItem(this, maxStock, stock)

  override val index: Int get() = ordinal
  override val itemType: ItemType get() = ItemType.TOOL

  companion object {
    fun fromOrdinal(ordinal: Int): Material = Material.entries[ordinal]
  }
}
