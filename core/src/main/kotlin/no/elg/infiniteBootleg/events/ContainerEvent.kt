package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.Event
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.OwnedContainer

sealed interface ContainerEvent : Event {

  val container: Container

  /**
   * Fired when the content of the container has changed
   */
  data class Changed(override val container: Container) : ContainerEvent

  data class Added(val ownedContainer: OwnedContainer) : ContainerEvent {
    override val container: Container get() = ownedContainer.container
  }

  data class Removed(val ownedContainer: OwnedContainer) : ContainerEvent {
    override val container: Container get() = ownedContainer.container
  }
}
