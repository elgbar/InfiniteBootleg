package no.elg.infiniteBootleg.events.api

import no.elg.infiniteBootleg.events.api.EventManager.eventsTracker
import no.elg.infiniteBootleg.events.api.EventManager.oneShotStrongRefs
import no.elg.infiniteBootleg.events.api.EventManager.strongListeners
import no.elg.infiniteBootleg.events.api.EventManager.weakListeners
import no.elg.infiniteBootleg.events.api.RegisteredEventListener.Companion.createRegisteredEventListener
import kotlin.reflect.KClass

/**
 * A registered event listener, primarily intended to make it possible to remove listeners without knowing what type of event it represents
 *
 * The constructor is private to prevent wong use, use [createRegisteredEventListener] to create new instances
 */
class RegisteredEventListener private constructor(
  private val listener: EventListener<out Event>,
  private val eventClass: KClass<out Event>
) {

  /**
   * Removes the listener
   */
  fun removeListener() {
    strongListeners.getIfPresent(eventClass)?.invalidate(listener)
    weakListeners.getIfPresent(eventClass)?.invalidate(listener)
    oneShotStrongRefs.invalidate(listener)
    eventsTracker?.onListenerUnregistered(eventClass, listener)
  }

  companion object {
    /**
     * Type safe method to create [RegisteredEventListener]
     */
    fun <T : Event> createRegisteredEventListener(listener: EventListener<T>, eventClass: KClass<T>) = RegisteredEventListener(listener, eventClass)
  }
}
