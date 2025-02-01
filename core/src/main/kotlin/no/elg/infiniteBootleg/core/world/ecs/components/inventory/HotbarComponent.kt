package no.elg.infiniteBootleg.core.world.ecs.components.inventory

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.ContainerElement
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.transients.RemoteEntityHoldingElement.Companion.remoteEntityHoldingElementOrNull
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.hotbar
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import java.util.EnumMap

private val logger = KotlinLogging.logger {}

data class HotbarComponent(var selected: HotbarSlot, val hotbarItems: EnumMap<HotbarSlot, Int>) : EntitySavableComponent {

  init {
    // Make sure the hotbar is filled with empty slots if there is nothing else there
    HotbarSlot.entries.forEach { hotbarItems.putIfAbsent(it, it.ordinal) }
  }

  /**
   * @return The index of the selected item in the entities container
   */
  val selectedIndex: Int get() = hotbarItems.getOrDefault(selected, EMPTY_INDEX)

  fun selectedItem(entity: Entity): Item? {
    val containerComponent = entity.containerOrNull ?: return null
    val index = selectedIndex
    if (index !in 0 until containerComponent.size) {
      if (index != EMPTY_INDEX) {
        logger.warn { "Invalid index $index for entity ${entity.nameOrNull ?: this}" }
      }
      return null
    }
    return containerComponent[index]
  }

  override fun hudDebug(): String = selected.name

  companion object : EntityLoadableMapper<HotbarComponent>() {

    /**
     * Indicate that a hotbar slot is not referencing anything in the inventory
     */
    const val EMPTY_INDEX: Int = -1

    var Entity.hotbarComponentOrNull by optionalPropertyFor(mapper)

    /**
     * @return The index of the selected item in the container
     */
    val Entity.selectedIndex: Int?
      get() {
        val hotbarComponent = hotbarComponentOrNull ?: return null
        return hotbarComponent.selectedIndex
      }

    /**
     * @return The selected item in the entity container or `null` if there is no selected item (or the index is invalid)
     */
    val Entity.selectedItem: Item? get() = hotbarComponentOrNull?.selectedItem(this)

    /**
     * @return The selected element in the entity container or the [remoteEntityHoldingElementOrNull]
     */
    val Entity.selectedElement: ContainerElement? get() = selectedItem?.element ?: remoteEntityHoldingElementOrNull

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) =
      safeWith {
        val protoHotbar = protoEntity.hotbar
        val map: EnumMap<HotbarSlot, Int> = EnumMap(HotbarSlot::class.java)
        protoHotbar.hotbarItemsList.forEachIndexed { index, itemIndex ->
          map[HotbarSlot.fromOrdinal(index)] = itemIndex
        }
        HotbarComponent(HotbarSlot.fromOrdinal(protoHotbar.selected), map)
      }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasHotbar() && hasOwnedContainer()

    enum class HotbarSlot {
      ONE,
      TWO,
      THREE,
      FOUR,
      FIVE,
      SIX,
      SEVEN,
      EIGHT,
      NINE;

      companion object {
        fun fromOrdinal(ordinal: Int): HotbarSlot = entries[ordinal]
        fun fromOrdinalOrNull(ordinal: Int): HotbarSlot? = entries.getOrNull(ordinal)
      }
    }
  }

  override fun EntityKt.Dsl.save() {
    hotbar = hotbar {
      selected = this@HotbarComponent.selected.ordinal
      hotbarItems += this@HotbarComponent.hotbarItems
        .map { (slot, index) -> slot.ordinal to index }
        .sortedBy { it.first }
        .map { it.second }
    }
  }
}
