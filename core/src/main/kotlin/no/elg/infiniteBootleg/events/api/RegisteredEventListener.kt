package no.elg.infiniteBootleg.events.api

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.events.api.EventManager.eventsTracker
import no.elg.infiniteBootleg.events.api.EventManager.strongListeners
import no.elg.infiniteBootleg.events.api.EventManager.weakListeners
import no.elg.infiniteBootleg.events.api.RegisteredEventListener.Companion.createRegisteredEventListener
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

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
    val eventListeners = synchronized(weakListeners) { weakListeners[eventClass] } ?: strongListeners[eventClass] ?: return
    synchronized(eventListeners) {
      if (eventListeners.remove(listener)) {
        eventsTracker?.onListenerUnregistered(eventClass, listener)
      } else {
        logger.warn { "Failed to remove listener (type $eventClass) $listener" }
      }
    }
  }

  companion object {
    /**
     * Type safe method to create [RegisteredEventListener]
     */
    fun <T : Event> createRegisteredEventListener(listener: EventListener<T>, eventClass: KClass<T>) = RegisteredEventListener(listener, eventClass)
  }
}
