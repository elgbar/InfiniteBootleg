package no.elg.infiniteBootleg.inventory.ui

import no.elg.infiniteBootleg.items.Item

/**
 * @author Daniel Holderbaum
 */
class Slot(val containerActor: ContainerActor, var stack: Item?, val index: Int) {
  constructor(slot: Slot) : this(slot.containerActor, slot.stack, slot.index)

  val isEmpty: Boolean
    get() = stack == null

  fun updateStack() {
    stack = containerActor.container[index]
  }
}
