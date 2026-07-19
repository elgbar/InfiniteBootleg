package no.elg.infiniteBootleg.core.items

import no.elg.infiniteBootleg.core.world.FistToolData
import no.elg.infiniteBootleg.core.world.Tool

/**
 * Represent the players fists which is the fallback tool if nothing is selected
 */
object ToolItemFist : ToolItem<FistToolData> {

  override val element get() = Tool.Fist
  override val data: FistToolData = FistToolData
  override val maxStock: UInt get() = UInt.MAX_VALUE
  override val stock: UInt get() = UInt.MAX_VALUE

  override fun remove(usages: UInt): ToolItemFist = ToolItemFist
  override fun equals(other: Any?): Boolean = other === this
  override fun hashCode(): Int = 0
}
