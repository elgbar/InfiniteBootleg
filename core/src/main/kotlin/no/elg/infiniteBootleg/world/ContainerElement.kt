package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.items.Item.Companion.DEFAULT_MAX_STOCK
import no.elg.infiniteBootleg.items.ItemType
import no.elg.infiniteBootleg.protobuf.ElementKt.enumElement
import no.elg.infiniteBootleg.protobuf.element
import no.elg.infiniteBootleg.util.valueOfOrNull
import no.elg.infiniteBootleg.world.Staff.Companion.fromProto
import no.elg.infiniteBootleg.world.Staff.Companion.toProto
import no.elg.infiniteBootleg.world.ecs.api.ProtoConverter
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Element as ProtoElement

sealed interface ContainerElement {

  val textureRegion: RotatableTextureRegion?

  val itemType: ItemType

  val displayName: String get() = textureRegion?.name ?: this::class.simpleName ?: itemType.name

  fun toItem(maxStock: UInt = DEFAULT_MAX_STOCK, stock: UInt = DEFAULT_MAX_STOCK): Item

  companion object : ProtoConverter<ContainerElement, ProtoElement> {

    fun valueOf(name: String): ContainerElement? = valueOfOrNull<Material>(name) ?: valueOfOrNull<Tool>(name)

    override fun ProtoElement.fromProto(): ContainerElement {
      return when (itemType) {
        ProtoElement.ItemType.MATERIAL -> Material.fromOrdinal(enumElement.index)
        ProtoElement.ItemType.TOOL -> Tool.fromOrdinal(enumElement.index)
        ProtoElement.ItemType.STAFF -> staff.fromProto()
        else -> error("Unknown item type $itemType")
      }
    }

    override fun ContainerElement.asProto(): ProtoElement =
      element {
        when (this@asProto) {
          is Material -> {
            itemType = ProtoElement.ItemType.MATERIAL
            enumElement = enumElement {
              index = ordinal
            }
          }

          is Tool -> {
            itemType = ProtoElement.ItemType.TOOL
            enumElement = enumElement {
              index = ordinal
            }
          }

          is Staff -> {
            itemType = ProtoElement.ItemType.STAFF
            staff = this@asProto.toProto()
          }
        }
      }
  }
}
