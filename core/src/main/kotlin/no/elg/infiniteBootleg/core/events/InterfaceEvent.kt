package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.events.api.Event
import no.elg.infiniteBootleg.core.inventory.container.InterfaceId

sealed interface InterfaceEvent : Event {

  val interfaceId: InterfaceId

  /**
   * Fired just before an interface is shown
   */
  data class Opening(override val interfaceId: InterfaceId) : InterfaceEvent

  /**
   * Fired just after an interface is hidden
   */
  data class Closed(override val interfaceId: InterfaceId) : InterfaceEvent

  /**
   * Fired after a new interface has been created
   */
  data class Added(override val interfaceId: InterfaceId) : InterfaceEvent

  /**
   * Fired after an interface is removed
   */
  data class Removed(override val interfaceId: InterfaceId) : InterfaceEvent
}
