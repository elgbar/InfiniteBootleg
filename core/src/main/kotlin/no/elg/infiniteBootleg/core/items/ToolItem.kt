package no.elg.infiniteBootleg.core.items

import no.elg.infiniteBootleg.core.world.Tool
import no.elg.infiniteBootleg.core.world.ToolData

interface ToolItem<DATA : ToolData> : Item {
  override val element: Tool<DATA>
  val data: DATA
}
