package no.elg.infiniteBootleg.inventory.container

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.items.Item

/**
 * @author kheba
 */
interface Inventory {
  /**
   * @return The item currently active
   */
  fun holding(): Item?

  /** Select the next item  */
  fun next()

  /** Select the previous item  */
  fun prev()

  /**
   * @return If the inventory is currently open
   */
  val isOpen: Boolean

  /** Select the `index`-th item  */
  fun select(index: Int)

  /**
   * @return The index of the selected item
   */
  val selected: Int

  /**
   * @return The entity that holds this container. Only this entity should have access to the
   * container, if it is `null` everyone should be able to modify and see the content of
   * this container
   */
  val holder: Entity?

  /**
   * @param entity The entity to check for ownership
   * @return If the given entity can view/modify the content of the storage
   */
  fun isHolder(entity: Entity?): Boolean {
    return holder == null || holder == entity
  }

  /** Open a GUI of this inventory  */
  fun open()

  /** Close the GUI of this inventory  */
  fun close()

  fun toggle() {
    if (isOpen) {
      close()
    } else {
      open()
    }
  }

  /**
   * @return The size of the storage
   */
  val size: Int

  /**
   * @return The container of this inventory
   */
  val container: Container?
}
