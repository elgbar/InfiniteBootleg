package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.events.api.Event
import no.elg.infiniteBootleg.core.inventory.container.Container
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer

sealed interface ContainerEvent : Event {

  val container: Container
  val owner: ContainerOwner?

  sealed interface OwnedContainerEvent : ContainerEvent {
    val ownedContainer: OwnedContainer
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

  data class Removed(override val ownedContainer: OwnedContainer) : OwnedContainerEvent
  data class Added(override val ownedContainer: OwnedContainer) : OwnedContainerEvent
}
