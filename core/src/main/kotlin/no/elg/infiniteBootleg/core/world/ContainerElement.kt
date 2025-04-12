package no.elg.infiniteBootleg.core.world

import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.items.ItemType
import no.elg.infiniteBootleg.core.world.Staff.Companion.fromProto
import no.elg.infiniteBootleg.core.world.Staff.Companion.toProto
import no.elg.infiniteBootleg.core.world.ecs.api.ProtoConverter
import no.elg.infiniteBootleg.protobuf.ElementKt.namedElement
import no.elg.infiniteBootleg.protobuf.element
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Element as ProtoElement

sealed interface TexturedContainerElement : ContainerElement {
  /**
   * The name of the texture used for this element
   */
  val textureName: String
}

sealed interface ContainerElement {

  val itemType: ItemType

  val displayName: String get() = (this as? Enum<*>)?.name ?: this::class.simpleName ?: itemType.name

  fun toItem(maxStock: UInt = Item.Companion.DEFAULT_MAX_STOCK, stock: UInt = Item.Companion.DEFAULT_MAX_STOCK): Item

  companion object : ProtoConverter<ContainerElement, ProtoElement> {

    fun valueOfOrNull(name: String): ContainerElement? = Material.valueOfOrNull(name) ?: Tool.valueOfOrNull(name)

    override fun ProtoElement.fromProto(): ContainerElement =
      when {
        hasMaterial() -> Material.valueOf(material.name)
        hasTool() -> Tool.valueOf(tool.name)
        hasStaff() -> staff.fromProto()
        else -> error("Unknown item type: $this")
      }

    override fun ContainerElement.asProto(): ProtoElement =
      element {
        when (this@asProto) {
          is Material -> material = namedElement {
            name = Material.nameOf(this@asProto)
          }

          is Tool -> tool = namedElement {
            name = Tool.nameOf(this@asProto)
          }

          is Staff -> staff = this@asProto.toProto()
        }
      }
  }
}
