package no.elg.infiniteBootleg.core.world.magic

import com.badlogic.ashley.core.Entity

interface Equippable {
  /**
   * Called when this is unequipped
   */
  fun onEquip(entity: Entity): Unit = Unit

  /**
   * Called when this is unequipped
   */
  fun onUnequip(entity: Entity): Unit = Unit
}
