package no.elg.infiniteBootleg.events

import no.elg.infiniteBootleg.events.api.Event
import no.elg.infiniteBootleg.inventory.container.Container

sealed interface ContainerEvent : Event {

  val container: Container

  /**
   * Fired when a container just before a container is shown
   */
  data class Opening(override val container: Container) : ContainerEvent

  /**
   * Fired when a container just after a container is hidden
   */
  data class Closed(override val container: Container) : ContainerEvent

  /**
   * Fired when the content of the container has changed
   */
  data class Changed(override val container: Container) : ContainerEvent
}
