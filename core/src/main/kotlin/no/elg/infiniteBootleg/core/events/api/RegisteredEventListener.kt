package no.elg.infiniteBootleg.core.events.api

import no.elg.infiniteBootleg.core.events.api.RegisteredEventListener.Companion.createRegisteredEventListener
import kotlin.reflect.KClass

/**
 * A registered event listener, primarily intended to make it possible to remove listeners without knowing what type of event it represents
 *
 * The constructor is private to prevent wong use, use [createRegisteredEventListener] to create new instances
 */
class RegisteredEventListener private constructor(private val listener: EventListener<out Event>, private val eventClass: KClass<out Event>) {

  /**
   * Removes the listener
   */
  fun removeListener() {
    EventManager.unregisteredListeners.incrementAndGet()
    EventManager.activeListeners.decrementAndGet()
    EventManager.weakListeners.getIfPresent(eventClass)?.invalidate(listener)
    EventManager.oneShotStrongRefs.invalidate(listener)
    EventManager.eventsTracker?.onListenerUnregistered(eventClass, listener)
  }

  companion object {
    /**
     * Type safe method to create [RegisteredEventListener]
     */
    fun <T : Event> createRegisteredEventListener(listener: EventListener<T>, eventClass: KClass<T>) = RegisteredEventListener(listener, eventClass)
  }
}
