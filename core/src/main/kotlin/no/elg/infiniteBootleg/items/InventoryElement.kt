package no.elg.infiniteBootleg.items

import no.elg.infiniteBootleg.items.Item.Companion.DEFAULT_MAX_STOCK
import no.elg.infiniteBootleg.protobuf.EntityKt.element
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.Tool
import no.elg.infiniteBootleg.world.ecs.api.ProtoConverter
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion

interface InventoryElement {

  val textureRegion: RotatableTextureRegion?

  val index: Int

  val itemType: ItemType

  fun toItem(maxStock: UInt = DEFAULT_MAX_STOCK, stock: UInt = DEFAULT_MAX_STOCK): Item

  companion object : ProtoConverter<InventoryElement, Entity.Element> {
    override fun Entity.Element.fromProto(): InventoryElement {
      return when (itemType) {
        Entity.Element.ItemType.MATERIAL -> Material.fromOrdinal(index)
        Entity.Element.ItemType.TOOL -> Tool.fromOrdinal(index)
        else -> throw IllegalArgumentException("Unknown inventory element type $itemType")
      }
    }

    override fun InventoryElement.asProto(): Entity.Element = element {
      index = this@asProto.index
      itemType = when (this@asProto) {
        is Material -> Entity.Element.ItemType.MATERIAL
        is Tool -> Entity.Element.ItemType.TOOL
        else -> throw IllegalArgumentException("Unknown inventory element type ${this@asProto::class.simpleName}")
      }
    }
  }
}
