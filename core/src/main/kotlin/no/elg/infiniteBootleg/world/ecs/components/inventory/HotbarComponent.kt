package no.elg.infiniteBootleg.world.ecs.components.inventory

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.hotbar
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.restriction.UniversalSystem
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import java.util.EnumMap

class HotbarComponent(var selected: HotbarSlot, val hotbarItems: EnumMap<HotbarSlot, Int>) : EntitySavableComponent, UniversalSystem {

  init {
    // Make sure the hotbar is filled with empty slots if there is nothing else there
    HotbarSlot.entries.forEach { hotbarItems.putIfAbsent(it, EMPTY_INDEX) }
  }

  /**
   * @return The index of the selected item in the entities container
   */
  val selectedIndex: Int get() = hotbarItems.getOrDefault(selected, EMPTY_INDEX)

  companion object : EntityLoadableMapper<HotbarComponent>() {

    /**
     * Indicate that a hotbar slot is not referencing anything in the inventory
     */
    const val EMPTY_INDEX: Int = -1

    var Entity.hotbarComponentOrNull by optionalPropertyFor(HotbarComponent.mapper)

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
    val Entity.selectedItem: Item?
      get() {
        val containerComponent = containerOrNull ?: return null
        val index = hotbarComponentOrNull?.selectedIndex ?: return null
        if (index !in 0 until containerComponent.size) {
          if (index != EMPTY_INDEX) {
            Main.logger().warn("Invalid index $index for entity ${this.nameOrNull ?: this}")
          }
          return null
        }
        return containerComponent[index]
      }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) =
      safeWith {
        val protoHotbar = protoEntity.hotbar
        val map: EnumMap<HotbarSlot, Int> = EnumMap(HotbarSlot::class.java)
        protoHotbar.hotbarItemsList.forEachIndexed { index, itemIndex ->
          map[HotbarSlot.fromOrdinal(index)] = itemIndex
        }
        HotbarComponent(HotbarSlot.fromOrdinal(protoHotbar.selected), map)
      }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasHotbar() && hasContainer()

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
        fun fromOrdinal(ordinal: Int) = entries[ordinal]
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
