package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.items.Item.Companion.DEFAULT_MAX_STOCK
import no.elg.infiniteBootleg.items.ItemType
import no.elg.infiniteBootleg.protobuf.EntityKt.ElementKt.enumElement
import no.elg.infiniteBootleg.protobuf.EntityKt.element
import no.elg.infiniteBootleg.world.Staff.Companion.fromProto
import no.elg.infiniteBootleg.world.Staff.Companion.toProto
import no.elg.infiniteBootleg.world.ecs.api.ProtoConverter
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity as ProtoEntity

sealed interface InventoryElement {

  val textureRegion: RotatableTextureRegion?

  val itemType: ItemType

  fun toItem(maxStock: UInt = DEFAULT_MAX_STOCK, stock: UInt = DEFAULT_MAX_STOCK): Item

  companion object : ProtoConverter<InventoryElement, ProtoEntity.Element> {
    override fun ProtoEntity.Element.fromProto(): InventoryElement {
      return when (itemType) {
        ProtoEntity.Element.ItemType.MATERIAL -> Material.fromOrdinal(enumElement.index)
        ProtoEntity.Element.ItemType.TOOL -> Tool.fromOrdinal(enumElement.index)
        ProtoEntity.Element.ItemType.STAFF -> staff.fromProto()
        else -> error("Unknown item type $itemType")
      }
    }

    override fun InventoryElement.asProto(): ProtoEntity.Element =
      element {
        when (this@asProto) {
          is Material -> {
            itemType = ProtoEntity.Element.ItemType.MATERIAL
            enumElement = enumElement {
              index = ordinal
            }
          }

          is Tool -> {
            itemType = ProtoEntity.Element.ItemType.TOOL
            enumElement = enumElement {
              index = ordinal
            }
          }

          is Staff -> {
            itemType = ProtoEntity.Element.ItemType.STAFF
            staff = this@asProto.toProto()
          }
        }
      }
  }
}
