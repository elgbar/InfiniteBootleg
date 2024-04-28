package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.Event
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.inventory.container.OwnedContainer

sealed interface ContainerEvent : Event {

  val container: Container
  val owner: ContainerOwner?

  sealed class OwnedContainerEvent(val ownedContainer: OwnedContainer) : ContainerEvent {
    override val owner: ContainerOwner
      get() = ownedContainer.owner
    override val container: Container
      get() = ownedContainer.container
  }

  /**
   * Fired when the content of the container has changed
   */
  data class Changed(override val container: Container, override val owner: ContainerOwner? = null) : ContainerEvent {
    constructor(ownedContainer: OwnedContainer) : this(ownedContainer.container, ownedContainer.owner)
  }

  class Removed(ownedContainer: OwnedContainer) : OwnedContainerEvent(ownedContainer)
  class Added(ownedContainer: OwnedContainer) : OwnedContainerEvent(ownedContainer)
}
