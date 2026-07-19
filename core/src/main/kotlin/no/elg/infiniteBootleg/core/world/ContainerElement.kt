package no.elg.infiniteBootleg.core.world

import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.items.ItemType

sealed interface TexturedContainerElement : ContainerElement {
  /**
   * The name of the texture used for this element
   */
  val textureName: String
}

/**
 * Something that can be held in a container/inventory by a player or creature
 */
sealed interface ContainerElement {

  val itemType: ItemType

  /**
   * If container element does not hold a state
   */
  val stateless: Boolean

  /**
   *
   * @return If this element can be handled by the player
   */
  val canBeHandled: Boolean

  val displayName: String get() = (this as? Enum<*>)?.name ?: this::class.simpleName ?: itemType.name

  fun toItem(maxStock: UInt = Item.DEFAULT_MAX_STOCK, stock: UInt = Item.DEFAULT_MAX_STOCK): Item

  companion object {

    fun valueOfOrNull(name: String): ContainerElement? = Material.valueOfOrNull(name) ?: Tool.valueOfOrNull(name)
  }
}
